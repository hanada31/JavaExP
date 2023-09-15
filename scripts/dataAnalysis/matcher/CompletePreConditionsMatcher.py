from .Base import Matcher
from .PreConditionsMatcher import PreConditionMatcher

class CompletePreConditionsMatcher(Matcher):
    def exception_equal(self, exception1, exception2):
        return PreConditionMatcher.pre_conditions_equal(exception1['preConditions'], exception2['preConditions'])