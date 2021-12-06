package org.rx.crawler;

import lombok.*;
import org.rx.core.EventArgs;
import org.rx.core.EventTarget;

import java.util.List;

public interface FiddlerWatcher extends EventTarget<FiddlerWatcher> {
    @Getter
    @RequiredArgsConstructor
    class CallbackEventArgs extends EventArgs {
        private final String key;
        private final List<String> content;

        public Object state;
    }

    String EVENT_CALLBACK = "onCallback";
}
