package net.hogedriven.backpaper0.radishtainer.test;

import javax.inject.Inject;

public class Ggg {

    public Aaa aaa = Aaa.INSTANCE;

    @Inject
    public Ggg(Aaa aaa) {
        this.aaa = aaa;
    }

    public Ggg() {
    }
}
