/* 
 * Copyright 2015 Rhythm & Hues Studios.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rhythm.louie.service.command;

import java.lang.reflect.*;
import java.util.*;

import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rhythm.louie.*;
import com.rhythm.louie.generator.ProcessorUtils;
import com.rhythm.louie.process.ServiceCall;
import com.rhythm.louie.request.RequestContext;
import com.rhythm.louie.request.data.*;
import com.rhythm.louie.request.data.DataParser.BuilderParser;
import com.rhythm.louie.service.AnnotatedService;

/**
 * @author cjohnson
 * Created: Oct 21, 2011 5:01:49 PM
 */
public class ReflectCommand implements PBCommand {
    private final Logger LOGGER = LoggerFactory.getLogger(ReflectCommand.class);
    
    private final AnnotatedService service;
    private final Method method;
    private final ServiceCall serviceCall;
    private final String description;
    private final PBParamType params;
    private final List<PBParamType> paramList;
    private final Descriptor returnType;
    private final Class<?> returnClass;
    private final boolean returnList;
    private final String returnDisplay;
    private final List<DataParser<?>> parsers;
    
    private final boolean isAnUpdater;
    private final Grouping group;
    private final boolean isInternal;
    private final boolean isStreaming;
    private final boolean deprecated;
    private final boolean adminAccess;
    private final boolean restrictedAccess;
    
    public ReflectCommand(final AnnotatedService service, final Method meth) throws Exception {
        this.service =service;
        this.method = meth;
        
        group = meth.isAnnotationPresent(Grouping.class) ? meth.getAnnotation(Grouping.class) : null;
        serviceCall = meth.getAnnotation(ServiceCall.class);
        
        isAnUpdater = meth.isAnnotationPresent(Updating.class);
        isInternal = meth.isAnnotationPresent(Internal.class);
        isStreaming = meth.isAnnotationPresent(Streaming.class);
        deprecated = meth.isAnnotationPresent(Deprecated.class);
        adminAccess = meth.isAnnotationPresent(Admin.class);
        restrictedAccess = meth.isAnnotationPresent(Restricted.class);
        
        description = ProcessorUtils.extractDescriptionFromJavadoc(serviceCall.javadoc());
        
        parsers = new ArrayList<>(meth.getParameterTypes().length);
        List<ArgType> args = new ArrayList<>(meth.getParameterTypes().length);
        int i=0;
        for (Class<?> arg : meth.getParameterTypes()) {
            Descriptor argDesc = getDescriptor(arg);
            args.add(new ArgType(argDesc,serviceCall.args()[i++]));
            parsers.add(new BuilderParser<>(getBuilder(arg)));
        }
        
        params = PBParamType.typeForArgs(args);
        paramList = Collections.singletonList(params);
        
        Type retType = meth.getGenericReturnType();
        if (retType instanceof Class) {
            returnClass = (Class)retType;
            returnType = getDescriptor(returnClass);
            returnList = false;
        } else if (retType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) retType;
            Type[] actualTypeArguments = paramType.getActualTypeArguments();
            if (Collection.class.isAssignableFrom(meth.getReturnType()) &&
                    actualTypeArguments != null && 
                    actualTypeArguments.length==1) {
                
                Type t = actualTypeArguments[0];
                if (t instanceof Class) {
                    returnClass = (Class)t;
                    returnType = getDescriptor(returnClass);
                } else {
                    throw new Exception("Not a Collection of a PB");
                }
                returnList = true;
            } else {
                throw new Exception("Not a single type Collection!  Don't know what to do!");
            }
            
        } else {
            LOGGER.error("UNKNOWN return type");
            returnClass = null;
            returnType = null;
            returnList = false;
        }
        
        returnDisplay = returnType==null?"":returnType.getFullName();
    }
    
    private static Descriptor getDescriptor(Class<?> cl) throws Exception {
        if (cl==null) {
            return null;
        }
        try {
            Method descMeth = cl.getMethod("getDescriptor");
            if (!Modifier.isStatic(descMeth.getModifiers())
                    || !Modifier.isPublic(descMeth.getModifiers())) {
                throw new Exception("Method getDescriptor is not public static.");
            }

            final Object o = descMeth.invoke(cl);
            if (!(o instanceof Descriptor)) {
                throw new Exception("Method getDescriptor did not return a PB Desriptor!");
            }
            return (Descriptor) o;
        } catch (Exception e) {
            throw new Exception("Class " + cl.getName() + " is not a PB!", e);
        }
    }
    
    private static Builder getBuilder(Class<?> cl) throws Exception {
        if (cl==null) {
            return null;
        }
        
        Method meth = cl.getMethod("newBuilder");
        if (!Modifier.isStatic(meth.getModifiers())
                || !Modifier.isPublic(meth.getModifiers())) {
            throw new Exception("Not a PB!");
        }
        Object o = meth.invoke(cl);
        if (!(o instanceof Builder)) {
            throw new Exception("Not a PB Descriptor!");
        }
        return (Builder) o;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Result execute(RequestContext request) throws Exception {
        try {
            if ((request.getParams().isEmpty() && params.getTypes().isEmpty()) || request.getParams().size() == 1) {
                Param param = request.getParams().isEmpty() ? Param.EMPTY : request.getParams().get(0);

                Object[] args;
                if (request.getParams().isEmpty()) {
                    args = new Object[]{};
                } else {
                    args = new Object[params.count()];
                    for (int i = 0; i < params.getTypes().size(); i++) {
                        args[i] = param.parseData(parsers.get(i), i);
                    }
                }

                Object o = method.invoke(service, args);
                if (o != null && o instanceof List) {
                    return Result.results(param, (List<? extends Message>) o);
                } else if (o == null || o instanceof Message) {
                    return Result.results(param, (Message) o);
                } else {
                    throw new Exception("Unknown return type!");
                }

            } else {
                LOGGER.warn("Multi Arg Request({}) {}:{}", request.getParams().size(),
                        request.getRequest().getService(), request.getRequest().getMethod());
                Map<Param, List<Message>> results = new HashMap<>();
                for (Param param : request.getParams()) {
                    Object[] args = new Object[params.count()];
                    for (int i = 0; i < params.getTypes().size(); i++) {
                        args[i] = param.parseData(parsers.get(i), i);
                    }

                    Object o = method.invoke(service, args);
                    if (o != null && o instanceof List) {
                        results.put(param, (List<Message>) o);
                    } else if (o == null || o instanceof Message) {
                        List<Message> argResults;
                        if (o == null) {
                            argResults = Collections.emptyList();
                        } else {
                            argResults = Collections.singletonList((Message)o);
                        }
                        results.put(param, argResults);
                    } else {
                        throw new Exception("Unknown return type!");
                    }
                }
                return Result.multiArgResults(results);
            }
        } catch (InvocationTargetException ie) {
            if (ie.getCause() != null && ie.getCause() instanceof Exception) {
                throw (Exception) ie.getCause();
            } else {
                throw ie;
            }
        }
    }
    
    @Override
    public String getCommandName() {
        return method.getName();
    } 

    @Override
    public String getDescription() {
        return description;
    }
    
    public String getJavadoc() {
        return serviceCall.javadoc();
    }

    @Override
    public List<PBParamType> getArguments() {
        return paramList;
    }
    
    @Override
    public String getReturnType() {
        return returnDisplay;
    }
    
    public Class<?> getReturnClass() {
        return returnClass;
    }

    public PBCommandType getCommandType() {
        return PBCommandType.valueOf(getCommandName(), params);
    }
    
    @Override
    public boolean isDeprecated() {
        return deprecated;
    }
    
    @Override
    public boolean returnList() {
        return returnList;
    }

    @Override
    public boolean isUpdate() {
        return isAnUpdater;
    }

    @Override
    public String getGroup() {
        if (group == null) return ""; //might not return null if group Obj is null
        return group.group();
    }

    @Override
    public int getGroupOrder() {
        if (group == null) return -1;
        return group.groupOrder();
    }

    @Override
    public boolean isInternal() {
        return isInternal;
    }
    
    @Override
    public boolean isStreaming() {
        return isStreaming;
    }

    @Override
    public boolean adminAccess() {
        return adminAccess;
    }

    @Override
    public boolean restrictedAccess() {
        return restrictedAccess;
    }
}
