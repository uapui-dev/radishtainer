package net.hogedriven.backpaper0.radishtainer.test;

import javax.inject.Provider;

public class WwwProvider implements Provider<Www> {

    @Override
    public Www get() {
        return Www.INSTANCE;
    }
}
