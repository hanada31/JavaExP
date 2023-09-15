from .Base import Matcher

class TypeMatcher(Matcher):
    def exception_equal(self, exception1, exception2):
        return exception1['exceptionName'] == exception2['exceptionName']
