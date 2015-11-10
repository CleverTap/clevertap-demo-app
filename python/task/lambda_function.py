from clevertap import CleverTap

import time
import datetime

import boto3
from boto3.dynamodb.conditions import Key, Attr

import random

import logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

TABLE_NAME = "quotes"
INDEX_NAME = "PersonalityTypeIndex"

CT_ACCOUNT_ID = "6Z8-64Z-644Z"
CT_ACCOUNT_PASSCODE = "WVE-SAD-OAAL"


QUOTE_BYTE_LENGTH = 40

try:
    clevertap = CleverTap(CT_ACCOUNT_ID, CT_ACCOUNT_PASSCODE)
except Exception, e:
    logger.error(e)
    clevertap = None

try:
    dynamo = boto3.resource('dynamodb').Table(TABLE_NAME)
except Exception, e:
    logger.error(e)
    dynamo = None

LENGTH_BY_PREFIX = [
  (0xC0, 2), # first byte mask, total codepoint length
  (0xE0, 3), 
  (0xF0, 4),
  (0xF8, 5),
  (0xFC, 6),
]

def codepoint_length(first_byte):
    if first_byte < 128:
        return 1 # ASCII
    for mask, length in LENGTH_BY_PREFIX:
        if first_byte & mask == mask:
            return length
    assert False, 'Invalid byte %r' % first_byte

def cut_to_bytes_length(unicode_text, byte_limit):
    utf8_bytes = unicode_text.encode('UTF-8')
    cut_index = 0
    while cut_index < len(utf8_bytes):
        step = codepoint_length(ord(utf8_bytes[cut_index]))
        if cut_index + step > byte_limit:
            return utf8_bytes[:cut_index]
        else:
            cut_index += step
    return utf8_bytes

def handler (event, context):

    if clevertap is None:
        logger.info("clevertap connection error")
        return False
    
    if dynamo is None:
        logger.info("dynamo connection error")
        return False

    # operate on some dates
    # from date is an arbitrary start date 
    from_date = 20000101

    utc_now = datetime.datetime.utcnow()
    fmt = '%Y%m%d'
    to_date = int(utc_now.strftime(fmt))

    # we always want to send to people at ~9am local time so calc their offset
    # add one as we will run this just before the hour
    hour_offset = 9 - (utc_now.hour+1)
    tz_string = "UTC%s" % hour_offset

    query = {'event_name': 'App Launched',
            'from': from_date,
            'to': to_date,
            'common_profile_prop': {
                'profile_fields': [
                    {'name': 'timeZone',
                        'operator': 'equals',
                        'value': tz_string
                        },

                    ]
                }
            }

    res = clevertap.profiles(query)

    quotes_cache = {}

    for profile_array in res:

        if len(profile_array) <= 0:
            continue

        profile = profile_array[0]
        object_id = profile.get("cookie", None)

        if not object_id:
            continue

        personality_type = profile.get("pv", {}).get("personalityType", "water")

        item = quotes_cache.get(personality_type, None)

        if item:
            quote = item['quote']
            quote_id = item['quote_id']

        else:
            try:
                query_response = dynamo.query(TableName=TABLE_NAME, IndexName=INDEX_NAME, KeyConditionExpression=Key('p_type').eq(personality_type))
                items = query_response.get("Items", [])
                item = random.choice(items)
                quote = item.get("quote")
                quote_id = item.get("quote_id")

            except Exception, e:
                logger.error(e)
                quote = None 
                quote_id = None 

            if quote and quote_id:
                # truncate quote if need be
                byte_length = len(quote.encode('utf-8'))
                if byte_length > QUOTE_BYTE_LENGTH:
                    quote = cut_to_bytes_length(quote, QUOTE_BYTE_LENGTH-3)
                    quote = quote +"..."
                    item['quote'] = quote

                quotes_cache[personality_type] = item 

        if not quote or not quote_id:
            continue
        
        # update profile and push actions into CT to trigger messaging
        data = []
        ts = int(time.time())
        has_email = profile.get("em", False)

        data.append({'type': 'profile',
                    'WZRK_G': object_id,
                    'ts': ts,
                    'profileData': {'quoteId': quote_id}
                    }) 

        data.append({'type': 'event',
                    'WZRK_G': object_id,
                    'ts': ts,
                    'evtName': 'newQuote',
                    'evtData': {"value":quote, "quoteId":quote_id}
                    })

        if has_email:
            data.append({'type': 'event',
                    'WZRK_G': object_id,
                    'ts': ts,
                    'evtName': 'newQuoteEmail',
                    'evtData': {"value":quote}
                    })

        if len(data) > 0:
            clevertap.up(data)
    
    return True    

if __name__ == '__main__':
    handler(None, None)
