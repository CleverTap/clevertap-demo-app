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
    if event and event.get("tz_string", None):
        tz_string = event.get("tz_string")
     
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

    for profile in res:

        # platformInfo is an array as a merged profile may have more than one platform (iOS, Android, Web)  associated with it
        # individual profiles that are merged via the same identity/email value will have multiple platformInfo dictionaries
        platformInfo = profile.get("platformInfo", []) 
        profileData = profile.get("profileData", None)

        if len(platformInfo) <= 0 or not profileData:
            continue

        # determine the object_id or identity value to use for the profile upload
        identity = None
        object_id = None

        # determine if we have an identity value
        email = profile.get("email", None)
        identity = profile.get("identity", email)

        if not identity:
            # if its a non-identified profile grab the object_id from the first platformInfo dict
            try:
                object_id = platformInfo[0].get("objectId", None)
            except Exception, e:
                logger.error(e)
                continue

        if not object_id and not identity:
            logger.error("no object_id or identity, continuing...")
            continue

        personality_type = profileData.get("personalityType", None)
        
        if not personality_type:
            continue
        
        # get the quote - cache based on personality type
        item = quotes_cache.get(personality_type, None)

        if item:
            quote = item['quote']
            quote_id = item['quote_id']

        # not in the cache - fetch from the db
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

        # set the new quote id on the user profile
        profile_push = {'type': 'profile',
                    'ts': ts,
                    'profileData': {'quoteId': quote_id}
                    } 

        # add the identity value
        if identity is not None:
            profile_push['identity'] = identity

        elif object_id is not None:
            profile_push['objectId'] = object_id

        else:
            continue

        data.append(profile_push)

        # if messaging enabled, push trigger events to CT
        # set our messaging availability flags
        # push defaults to True
        can_push = profileData.get("canPush", True)

        # if email + explicity canEmail set to True then trigger email for this user
        can_email = email is not None
        if can_email:
            # email defaults to False
            can_email = profileData.get("canEmail", False)
        
        if can_push:
            push_data = {'type': 'event',
                        'ts': ts,
                        'evtName': 'newQuote',
                        'evtData': {"value":quote, "quoteId":quote_id}
                        }

            # add the identity value
            if identity is not None:
                push_data['identity'] = identity

            elif object_id is not None:
                push_data['objectId'] = object_id

            else:
                continue

            data.append(push_data)
    
        if can_email:
            email_data = {'type': 'event',
                        'ts': ts,
                        'evtName': 'newQuoteEmail',
                        'evtData': {"value":quote, "quoteId":quote_id}
                        }

            # add the identity value
            if identity is not None:
                email_data['identity'] = identity

            elif object_id is not None:
                email_data['objectId'] = object_id

            else:
                continue

            data.append(email_data)

        if len(data) > 0:
            clevertap.upload(data)
        
    return True    

if __name__ == '__main__':
    handler({"tz_string":"UTC-7"}, None)
