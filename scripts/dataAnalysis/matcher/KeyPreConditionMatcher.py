from .Base import Matcher
from .PreConditionsMatcher import PreConditionMatcher

class KeyPreConditionMatcher(Matcher):
    def exception_equal(self, exception1, exception2):
        return PreConditionMatcher.subset_list_match(exception1['keyPreCondition'], exception2['keyPreCondition'])