import boto3
import json

import logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

TABLE_NAME = "quotes"

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
            quote = response.get("Item", {}).get("quote", None)
        except Exception, e:
            logger.error(e) 

        return quote

    else:
        raise ValueError("unknown operation %s" % operation)
