from abc import ABC, abstractmethod

class Matcher(ABC):
    def match(self, exceptions: list, target_exception):
        matched_exceptions = self.match_exception(exceptions, target_exception)
        if len(matched_exceptions) == 0:
            return []
        else:
            return matched_exceptions

    def match_exception(self, exceptions: list, target_exception):
        matched_exceptions = []
        for exception in exceptions:
            if self.exception_equal(exception, target_exception):
                matched_exceptions.append(exception)
        return matched_exceptions

    @abstractmethod
    def exception_equal(self, exception1, exception2):
        pass
