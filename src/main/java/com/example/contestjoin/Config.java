package com.example.contestjoin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "contest-join")
public class Config {
    private int batchSize;
    private int maxAvailableSpots;

    public Config(int batchSize, int maxAvailableSpots) {
        this.batchSize = batchSize;
        this.maxAvailableSpots = maxAvailableSpots;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxAvailableSpots() {
        return maxAvailableSpots;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setMaxAvailableSpots(int maxAvailableSpots) {
        this.maxAvailableSpots = maxAvailableSpots;
    }
}
