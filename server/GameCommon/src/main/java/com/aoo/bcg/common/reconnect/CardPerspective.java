package com.aoo.bcg.common.reconnect;

import java.util.ArrayList;
import java.util.List;

/** Builds a detached hand view and masks every card not visible to the viewer. */
public final class CardPerspective {
    private CardPerspective() {}

    public static ArrayList<Integer> hand(List<Integer> authoritativeCards, boolean visible) {
        ArrayList<Integer> view = new ArrayList<>(authoritativeCards.size());
        if (visible) {
            view.addAll(authoritativeCards);
        } else {
            for (int ignored : authoritativeCards) view.add(0);
        }
        return view;
    }
}
