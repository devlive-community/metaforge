package org.devlive.metaforge.notify

import com.google.inject.multibindings.Multibinder

class TestNotifyModule : NotifyModule()
{
    override fun configure()
    {
        Multibinder.newSetBinder(this.binder(), Notify::class.java)
            .addBinding()
            .to(TestNotify::class.java)
    }
}
