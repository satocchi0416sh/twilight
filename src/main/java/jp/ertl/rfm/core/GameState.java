package jp.ertl.rfm.core;

public enum GameState {
    IDLE("待機中"),
    LOBBY("ロビー"),
    COUNTDOWN("カウントダウン"),
    RUNNING("ゲーム中"),
    PAUSED("一時停止"),
    ENDING("終了処理中"),
    RESULT("結果表示");
    
    private final String displayName;
    
    GameState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isActive() {
        return this == RUNNING || this == PAUSED;
    }
    
    public boolean canJoin() {
        return this == LOBBY || this == COUNTDOWN;
    }
}