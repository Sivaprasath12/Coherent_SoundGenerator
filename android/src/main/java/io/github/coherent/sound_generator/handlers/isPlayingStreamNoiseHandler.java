package io.github.coherent.sound_generator.handlers;

import io.flutter.plugin.common.EventChannel;

public class isPlayingStreamNoiseHandler implements EventChannel.StreamHandler {
        public static final String NATIVE_CHANNEL_EVENT = "io.github.coherent.sound_generator/onChangeIsPlayingnoise";
        private volatile static io.github.coherent.sound_generator.handlers.isPlayingStreamNoiseHandler mEventManager;
        EventChannel.EventSink eventSink;

        public isPlayingStreamNoiseHandler()
        {
            if(mEventManager == null)
                mEventManager = this;
        }

        @Override
        public void onListen(Object o, EventChannel.EventSink eventSink) {
            this.eventSink = eventSink;
        }

        public static void change(boolean value) {
            if(mEventManager != null)
                if (mEventManager.eventSink != null)
                    mEventManager.eventSink.success(Boolean.valueOf(value));
        }

        @Override
        public void onCancel(Object o) {
            if (this.eventSink != null) {
                this.eventSink.endOfStream();
                this.eventSink = null;
            }
        }
    }
