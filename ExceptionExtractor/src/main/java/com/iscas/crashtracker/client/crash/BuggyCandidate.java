package com.iscas.crashtracker.client.crash;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.annotation.JSONField;
import com.iscas.crashtracker.utils.PrintUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuggyCandidate {
    @JSONField(name = "Candidate Name")
    private String candidateName;
    @JSONField(name = "Candidate Score")
    private final int candidateScore;
    @JSONField(name = "Reasons")
    private JSONArray reasons = new JSONArray();
    private Set<String> reasonTrace = new HashSet<String>();

    public int getcandidateScore() {
        return candidateScore;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public JSONArray getReasons() {
        return reasons;
    }

    public BuggyCandidate(String candi, int score){
        this.candidateName = candi;
        this.candidateScore = score;
    }

    public void addReasonTrace(JSONObject reason) {
        if(reasonTrace.contains(reason.toJSONString()))
            return;
        reasonTrace.add(reason.toJSONString());
        reasons.add(reason);
    }

}
