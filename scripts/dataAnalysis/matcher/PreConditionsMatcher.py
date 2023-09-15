from .Base import Matcher

class PreConditionMatcher(Matcher):
    @staticmethod
    def unorder_list_equal(list1: list, list2: list):
        if len(list1) != len(list2):
            return False
        return PreConditionMatcher.subset_list_match(list1, list2)
    
    @staticmethod
    def subset_list_match(list1: list, list2: list):
        if len(list1) > len(list2):
            list1, list2 = list2, list1
        for item in list1:
            if item not in list2:
                return False
        return True

    @staticmethod
    def pre_conditions_equal(pre_conditions_1, pre_conditions_2):
        if len(pre_conditions_1) != len(pre_conditions_2):
            return False
        return PreConditionMatcher.pre_conditions_match(pre_conditions_1, pre_conditions_2)
    
    @staticmethod
    def pre_conditions_match(pre_conditions_1, pre_conditions_2):
        cond_list1 = pre_conditions_1.copy()
        cond_list2 = pre_conditions_2.copy()
        if len(cond_list1) > len(cond_list2):
            cond_list1, cond_list2 = cond_list2, cond_list1
        for cond1 in cond_list1:
            match = False
            for cond2 in cond_list2.copy():
                if PreConditionMatcher.unorder_list_equal(cond1, cond2):
                    match = True
                    cond_list2.remove(cond2)
                    break
            if match is False:
                return False
        return True
    
    @staticmethod
    def subset_match(pre_conditions_1, pre_conditions_2):
        cond_list1 = pre_conditions_1.copy()
        cond_list2 = pre_conditions_2.copy()
        if len(cond_list1) > len(cond_list2):
            cond_list1, cond_list2 = cond_list2, cond_list1
        
        # completely match
        for cond1 in cond_list1.copy():
            matched_condition = None
            for cond2 in cond_list2.copy():
                if PreConditionMatcher.subset_list_match(cond1, cond2):
                    if matched_condition is None:
                        matched_condition = cond2
                    else:
                        standard_len = len(cond1)
                        old_len = abs(len(matched_condition) - standard_len)
                        new_len = abs(len(cond2) - standard_len)
                        if new_len < old_len:
                            matched_condition = cond2
                
            if matched_condition is None:
                return False
            else:
                cond_list2.remove(matched_condition)
        return True

    def exception_equal(self, exception1, exception2):
        return PreConditionMatcher.subset_match(exception1['preConditions'], exception2['preConditions'])
