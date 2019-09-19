package com.why.springbootpractice.providerimpl;

import com.why.springbootpractice.provider.DemoProvider;
import org.springframework.stereotype.Service;

@Service("demoProvider")
public class DemoProviderImpl implements DemoProvider {
    @Override
    public String say(String name) {
        return "Get back: " + name;
    }
}
