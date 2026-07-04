package com.example.cardgame;

import android.app.Application;

import com.example.cardgame.controller.narrative.NarrativeActionHandler;
import com.example.cardgame.controller.narrative.NarrativeGameController;

public class CardGameApplication extends Application {

    private static NarrativeActionHandler narrativeActionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        narrativeActionHandler = new NarrativeGameController();
    }

    public static NarrativeActionHandler getNarrativeActionHandler() {
        return narrativeActionHandler;
    }
}