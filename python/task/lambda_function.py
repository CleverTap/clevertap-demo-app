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

def handler (event, context):

    if clevertap is None:
        logger.info("clevertap connection error")
        return False
    
    if dynamo is None:
        logger.info("dynamo connection error")
        #return False

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

    for profile_array in res:
        for profile in profile_array:
            object_id = profile.get("cookie", None)

            if not object_id:
                continue

            personality_type = profile.get("pv", {}).get("personalityType", "water")

            # TODO: prevent dupes

            try:
                query_response = dynamo.query(TableName=TABLE_NAME, IndexName=INDEX_NAME, KeyConditionExpression=Key('p_type').eq(personality_type))
                logger.info(query_response)
                items = query_response.get("Items", [])
                item = random.choice(items)
                quote = item.get("quote")
                quote_id = item.get("quote_id")
            except Exception, e:
                logger.error(e)
                quote = None 
                quote_id = None 

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
                        'evtData': {"value":quote}
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