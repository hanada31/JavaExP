from .Base import Matcher

class MessageMatcher(Matcher):
    def exception_equal(self, exception1, exception2):
        return exception1['message'] == exception2['message']
