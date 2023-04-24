package com.techsure.autoexecrunner.tagent;


import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TagentHandlerFactory {
    private static Map<String, TagentHandlerBase> actionMap = new HashMap<String, TagentHandlerBase>();

    static {
        Reflections reflections = new Reflections("com.techsure.autoexecrunner.tagent.handler");
        Set<Class<? extends TagentHandlerBase>> modules = reflections.getSubTypesOf(TagentHandlerBase.class);
        for (Class<? extends TagentHandlerBase> c : modules) {
            TagentHandlerBase action;
            try {
                action = c.newInstance();
                actionMap.put(action.getName(), action);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }

    public static TagentHandlerBase getAction(String type) {
        return actionMap.get(type);
    }
}
