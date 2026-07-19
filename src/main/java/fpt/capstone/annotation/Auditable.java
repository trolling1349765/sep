package fpt.capstone.annotation;

import fpt.capstone.enums.Action;
import fpt.capstone.enums.Table;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

    Action action();

    Table entity();
}
