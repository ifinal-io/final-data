/*
 * Copyright 2020-2022 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ifinalframework.data.mybatis.reflection;

import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * FinalPropertyTokenizer.
 *
 * @author ilikly
 * @version 1.3.3
 * @since 1.3.3
 */
public class FinalPropertyTokenizer extends PropertyTokenizer {
    private String name;
    private final String indexedName;
    private String index;
    private final String children;

    public FinalPropertyTokenizer(String fullname) {
        super(fullname);
        int delim = fullname.indexOf('.');
        int delim2 = fullname.indexOf('[');

        if(delim > -1 && delim2 > -1){
            delim = Math.min(delim,delim2);
            name = fullname.substring(0, delim);
            children = fullname.substring(delim + 1);
        }else if(delim > -1){
            name = fullname.substring(0, delim);
            children = fullname.substring(delim + 1);
        }else if(delim2 > -1){
            name = fullname.substring(0, delim2);
            children = fullname.substring(delim2 + 1);
        }else {
            name = fullname;
            children = null;
        }

        indexedName = name;
        delim = name.indexOf('[');
        if (delim > -1) {
            index = name.substring(delim + 1, name.length() - 1);
            name = name.substring(0, delim);
        }
    }

    public String getName() {
        return name;
    }

    public String getIndex() {
        return index;
    }

    public String getIndexedName() {
        return indexedName;
    }

    public String getChildren() {
        return children;
    }

    @Override
    public boolean hasNext() {
        return children != null;
    }

    @Override
    public PropertyTokenizer next() {
        return new PropertyTokenizer(children);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported, as it has no meaning in the context of properties.");
    }
}


