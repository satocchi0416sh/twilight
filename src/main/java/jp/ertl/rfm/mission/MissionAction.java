package jp.ertl.rfm.mission;

import java.util.Map;

public class MissionAction {
    private final String type;
    private final Map<String, Object> params;
    
    public MissionAction(String type, Map<String, Object> params) {
        this.type = type;
        this.params = params;
    }
    
    public String getType() {
        return type;
    }
    
    public Map<String, Object> getParams() {
        return params;
    }
}