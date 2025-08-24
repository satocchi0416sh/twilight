package jp.ertl.rfm.mission;

import java.util.ArrayList;
import java.util.List;

public class Mission {
    
    public enum TriggerType {
        TIME,
        PLAYER_COUNT,
        LOCATION,
        INTERACT,
        RANDOM,
        CUSTOM
    }
    
    private final String id;
    private final String name;
    private final String description;
    private TriggerType triggerType;
    private String triggerValue;
    private final List<MissionAction> actions;
    private int duration;
    private boolean repeatable;
    
    public Mission(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.actions = new ArrayList<>();
        this.duration = 60;
        this.repeatable = false;
    }
    
    public void addAction(MissionAction action) {
        actions.add(action);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public TriggerType getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }
    
    public String getTriggerValue() {
        return triggerValue;
    }
    
    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }
    
    public List<MissionAction> getActions() {
        return actions;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public boolean isRepeatable() {
        return repeatable;
    }
    
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
}