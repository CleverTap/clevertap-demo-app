import unittest
import time
from clevertap import CleverTap

CT_ACCOUNT_ID = "6Z8-64Z-644Z"
CT_ACCOUNT_PASSCODE = "WVE-SAD-OAAL"

class CleverTapTests(unittest.TestCase):

    def setUp(self):
        self.clevertap = CleverTap(CT_ACCOUNT_ID, CT_ACCOUNT_PASSCODE)

    def test_upload(self):
        data = [
                {'type': 'profile',
                    'WZRK_G': "_353918052239536",
                    'ts': int(time.time()),
                    'profileData': {'quoteID': '12345678'}
                    }, 

                {'type': 'event',
                    'WZRK_G': "_353918052239536",
                    'ts': int(time.time()),
                    'evtName': 'newQuote',
                    'evtData': {"value":"A B C D E F G H I J K"}
                    },

                {'type': 'event',
                    'WZRK_G': "_353918052239536",
                    'ts': int(time.time()),
                    'evtName': 'newQuoteEmail',
                    'evtData': {"value":"A B C D E F G H I J K"}
                    },


                ]

        res = self.clevertap.up(data) or {}
        unprocessedRecords = res.get('unprocessedRecords:', [])
        self.assertEqual(len(unprocessedRecords), 0, '%s records failed' % len(unprocessedRecords))

    def test_download_profiles(self):
        query = {'event_name': 'App Launched',
                'from': 20150910,
                'to': 20151029,
                'common_profile_prop': {
                    'profile_fields': [
                        {'name': 'timeZone',
                            'operator': 'equals',
                            'value': 'UTC-7'
                            },

                        {'name': 'personalityType',
                            'operator': 'equals',
                            'value': 'metal'
                            },

                        ]
                    }
                }

        res = self.clevertap.profiles(query)
        self.assertTrue(len(res) > 0)


if __name__ == '__main__':
    suite = unittest.TestLoader().loadTestsFromTestCase(CleverTapTests)
    unittest.TextTestRunner(verbosity=2).run(suite)
