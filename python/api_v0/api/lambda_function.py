import boto3
from boto3.dynamodb.conditions import Key, Attr

import json
import random

import logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

TABLE_NAME = "quotes"
INDEX_NAME = "PersonalityTypeIndex"

def handler (event, context):
    '''Provide an event that contains the following keys:

      - operation: one of the operations below
      - quoteId: required for operations that interact with DynamoDB
    '''
    operation = event.get("operation", None)
    logger.info("running handler with operation %s" % json.dumps(event))

    if operation == 'ping': 
        return 'pong'

    elif operation == "fetchQuoteFromId":
        try:
            dynamo = boto3.resource('dynamodb').Table(TABLE_NAME)
        except Exception, e:
            logger.error(e)
            raise ValueError("db connection error")
        try:
            quoteId = event.get("quoteId", None)
        except Exception, e:
            logger.error(e)
            raise ValueError("invalid quote id")

        quote = None
        try:
            response = dynamo.get_item(TableName=TABLE_NAME, Key={"quote_id":quoteId})
            quote = response.get("Item", None)
        except Exception, e:
            logger.error(e) 

        return quote

    elif operation == "fetchQuoteForType":
        try:
            dynamo = boto3.resource('dynamodb').Table(TABLE_NAME)
        except Exception, e:
            logger.error(e)
            raise ValueError("db connection error")
        try:
            personality_type = event.get("p_type", None)
            if personality_type not in ['earth', 'fire', 'water', 'metal', 'wood']:
                raise ValueError()
        except Exception, e:
            logger.error(e)
            raise ValueError("invalid personality type")

        quote = None
        try:
            query_response = dynamo.query(TableName=TABLE_NAME, IndexName=INDEX_NAME, KeyConditionExpression=Key('p_type').eq(personality_type))
            items = query_response.get("Items", [])
            quote = random.choice(items)
        except Exception, e:
            print e
            logger.error(e) 

        return quote

    else:
        raise ValueError("unknown operation %s" % operation)

if __name__ == '__main__':
    res = handler({"operation":"fetchQuoteFromId", "quoteId":"1"}, None)
    print res
    res = handler({"operation":"fetchQuoteForType", "p_type":"metal"}, None)
    print res
