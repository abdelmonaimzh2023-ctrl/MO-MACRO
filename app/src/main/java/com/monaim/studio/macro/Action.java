package com.monaim.studio.macro;

public class Action {
    private String type;
    private float x, y, endX, endY;
    private long duration;
    private long delay;
    private int repeat;

    public Action() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getEndX() { return endX; }
    public void setEndX(float endX) { this.endX = endX; }

    public float getEndY() { return endY; }
    public void setEndY(float endY) { this.endY = endY; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getDelay() { return delay; }
    public void setDelay(long delay) { this.delay = delay; }

    public int getRepeat() { return repeat; }
    public void setRepeat(int repeat) { this.repeat = repeat; }
}
