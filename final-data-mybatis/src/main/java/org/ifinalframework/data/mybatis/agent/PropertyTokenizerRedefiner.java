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

package org.ifinalframework.data.mybatis.agent;

import org.apache.ibatis.reflection.property.PropertyTokenizer;

import org.ifinalframework.java.Redefiner;

/**
 * PropertyTokenizerAgent.
 *
 * @author ilikly
 * @version 1.3.3
 * @since 1.3.3
 */
public class PropertyTokenizerRedefiner {

    public static void redefine() {

        Redefiner.redefine(PropertyTokenizer.class,
                "package org.apache.ibatis.reflection.property;\n" +
                        "\n" +
                        "import java.util.Iterator;\n" +
                        "\n" +
                        "/**\n" +
                        " * @author Clinton Begin\n" +
                        " */\n" +
                        "public class PropertyTokenizer implements Iterator<PropertyTokenizer> {\n" +
                        "    private String name;\n" +
                        "    private final String indexedName;\n" +
                        "    private String index;\n" +
                        "    private final String children;\n" +
                        "\n" +
                        "    public PropertyTokenizer(String fullname) {\n" +
                        "        int delim = fullname.indexOf('.');\n" +
                        "        int delim2 = fullname.indexOf(\"][\");\n" +
                        "        if (delim > -1) {\n" +
                        "            name = fullname.substring(0, delim);\n" +
                        "            children = fullname.substring(delim + 1);\n" +
                        "        } else if (delim2 > -1) {\n" +
                        "            name = fullname.substring(0, delim2 + 1);\n" +
                        "            children = fullname.substring(delim2 + 1);\n" +
                        "        } else {\n" +
                        "            name = fullname;\n" +
                        "            children = null;\n" +
                        "        }\n" +
                        "        indexedName = name;\n" +
                        "        delim = name.indexOf('[');\n" +
                        "        if (delim > -1) {\n" +
                        "            index = name.substring(delim + 1, name.length() - 1);\n" +
                        "            name = name.substring(0, delim);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getName() {\n" +
                        "        return name;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getIndex() {\n" +
                        "        return index;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getIndexedName() {\n" +
                        "        return indexedName;\n" +
                        "    }\n" +
                        "\n" +
                        "    public String getChildren() {\n" +
                        "        return children;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public boolean hasNext() {\n" +
                        "        return children != null;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public PropertyTokenizer next() {\n" +
                        "        return new PropertyTokenizer(children);\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void remove() {\n" +
                        "        throw new UnsupportedOperationException(\"Remove is not supported, as it has no meaning in the context of properties.\");\n" +
                        "    }\n" +
                        "}\n");

    }

}


