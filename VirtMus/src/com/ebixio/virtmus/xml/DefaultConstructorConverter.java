/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebixio.virtmus.xml;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.Constructor;


/**
 * This can be used for classes that have data members that are declared as
 * "transient final". It invokes the default constructor to construct those
 * objects, because final members can not be initialized in readResolve().
 * 
 * <code>
 * XStream xs = new XStream();
 * xs.registerConverter(new DefaultConstructorConverter1(xs.getMapper(), xs.getReflectionProvider()));
 *</code>
 * 
 * @see <a href="http://www.blogs.uni-osnabrueck.de/rotapken/2010/08/20/let-xstream-call-the-default-constructor-where-possible/">
 * Additional documentation.</a>
 */
public class DefaultConstructorConverter extends ReflectionConverter {
    public DefaultConstructorConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
    }

    @Override
    public boolean canConvert(Class clazz) {
        for (Constructor c : clazz.getConstructors()) {
            if (c.getParameterTypes().length == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
        try {
            //Class clazz = Class.forName(reader.getNodeName());
            Class clazz = mapper.realClass(reader.getNodeName());
            return clazz.newInstance();
        } catch (Exception e) {
            throw new ConversionException("Could not create instance of class " + reader.getNodeName(), e);
        }
    }
}