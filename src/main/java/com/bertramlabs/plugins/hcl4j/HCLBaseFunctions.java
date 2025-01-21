package com.bertramlabs.plugins.hcl4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides implementations of the common Terraform Base HCL Functions that are commonly used.
 * Functions can easily be registered dynamically on the HCLParser as well but this particular set is auto registered in the
 * constructor of the {@link HCLParser}
 * TODO: Not all functions have been implemented yet. Most functions currently fail by returning null to prevent errors from occurring
 * during high level processing. This could be changed in the future to be an optional behavior of the parser
 *
 * @since 0.5.0
 * @author David Estes
 */
public class HCLBaseFunctions {
    static void registerBaseFunctions(HCLParser parser) {
        parser.registerFunction("upper", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) != null) {
                return arguments.get(0).toString().toUpperCase(Locale.ROOT);
            }
            return null;
        });

        parser.registerFunction("lower", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) != null) {
                return arguments.get(0).toString().toLowerCase(Locale.ROOT);
            }
            return null;
        });

        //TODO: Implement this function
//        parser.registerFunction("can", (arguments) -> {
//            if(arguments.size() > 0 && arguments.get(0) != null) {
//                return arguments.get(0) != null;
//            }
//            return false;
//        });

        parser.registerFunction("format", (arguments) -> {
            if(arguments.size() > 0  && arguments.get(0) != null) {
                String val = arguments.get(0).toString();
                ArrayList<Object> args = new ArrayList<>();
                for(int x=1;x<arguments.size();x++) {
                    args.add(arguments.get(x));
                }
                return String.format(val,args.toArray());
            }
            return null;
        });


        parser.registerFunction("trimspace", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) != null) {
                return arguments.get(0).toString();
            }
            return null;
        });


        parser.registerFunction("trim", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) != null) {
                return arguments.get(0).toString();
            }
            return null;
        });


        parser.registerFunction("strrev", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) != null) {
                String str = arguments.get(0).toString();
                String reversed = "";
                char ch;
                for (int i=0; i<str.length(); i++)
                {
                    ch= str.charAt(i); //extracts each character
                    reversed= ch + reversed; //adds each character in front of the existing string
                }
                return reversed;
            }
            return null;
        });

        parser.registerFunction("regexall", (arguments) -> {
            if(arguments.size() > 1 && arguments.get(0) != null ) {
                String str = arguments.get(1) != null ? arguments.get(1).toString() : "";
                Pattern regex = Pattern.compile(arguments.get(0).toString());
                Matcher matcher = regex.matcher(str);
                //convert match list to array of results
                ArrayList<String> results = new ArrayList<>();
                while(matcher.find()) {
                    results.add(matcher.group());
                }
                return results;
            }
            return null;
        });

        parser.registerFunction("contains", (arguments) -> {
            if(arguments.size() == 2) {
                if(arguments.get(0) instanceof Collection) {
                    Collection list = (Collection)(arguments.get(0));
                    try {
                        return list.contains(arguments.get(1));
                    } catch(ClassCastException ex) {
                        return null;
                    }

                } else {
                    //invalid stuff
                    return null; //Invalid Function Spec
                }
            } else {
                return null; //Invalid Function Spec
            }

        });


        parser.registerFunction("split", (arguments) -> {
            if(arguments.size() == 2) {
                    String separator = (String)(arguments.get(0));
                    try {
                        String value = (String)(arguments.get(1));
                        ArrayList<String> elements = new ArrayList<>();
                        if(value == null) {
                            return null;
                        }
                        StringTokenizer tokenizer = new StringTokenizer(value,separator);
                        while(tokenizer.hasMoreTokens()) {
                            elements.add(tokenizer.nextToken());
                        }
                        return elements;
                    } catch(ClassCastException ex) {
                        return null;
                    }
                    
            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("join", (arguments) -> {
            if(arguments.size() == 2) {
                String separator = (String)(arguments.get(0));
                if(arguments.get(1) instanceof Collection) {
                    Collection list = (Collection)(arguments.get(1));
                    ArrayList<String> elements = new ArrayList<>();
                    for(Object listItem : list) {
                        elements.add(listItem.toString());
                    }
                    return String.join(separator,elements);
                } else {
                    return null;
                }

            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("startswith", (arguments) -> {
            if(arguments.size() == 2) {
                String prefix = (String)(arguments.get(1));
                String value = (String)(arguments.get(0));
                return value.startsWith(prefix);

            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("endswith", (arguments) -> {
            if(arguments.size() == 2) {
                String prefix = (String)(arguments.get(1));
                String value = (String)(arguments.get(0));
                return value.endsWith(prefix);
            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("substr", (arguments) -> {
            if(arguments.size() == 3) {
                Double offset = (Double)(arguments.get(1));
                Double length  = (Double)(arguments.get(2));
                String value = (String)(arguments.get(0));
                Integer endIndex = offset.intValue() + length.intValue();
                if(endIndex > value.length() - 1) {
                    endIndex = value.length();
                }
                if(length < 0) {
                    endIndex = value.length() + 1 + length.intValue();
                }
                if(offset < 0) {
                    offset = value.length() + offset;
                }
                return value.substring(offset.intValue(),endIndex);
            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("replace", (arguments) -> {
            if(arguments.size() == 3) {
                String substring = (String)(arguments.get(1));
                String replacement  = (String)(arguments.get(2));
                String value = (String)(arguments.get(0));
                if(value == null) {
                    return null;
                }
                return value.replace(substring,replacement);
            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("uuid", (arguments) -> {
            return java.util.UUID.randomUUID().toString();
        });

        parser.registerFunction("try", (arguments) -> {
            for(Object argument : arguments) {
                if(argument != null) {
                    return argument;
                }
            }
            return null;
        });

        registerNumericFunctions(parser);
        registerCollectionFunctions(parser);
        registerDateFunctions(parser);
        registerCastingFunctions(parser);
    }

    static void registerNumericFunctions(HCLParser parser) {
        parser.registerFunction("max", (arguments) -> {
            Double maxValue = null;
            for(Object argument : arguments) {
                if(argument instanceof Double) {
                    if(maxValue == null || (Double)argument > maxValue) {
                        maxValue = (Double) argument;
                    }
                }
            }
            return maxValue;
        });


        parser.registerFunction("tonumber", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) instanceof String) {
                Double val = null;
                try {
                    val = Double.parseDouble((String)(arguments.get(0)));
                } catch(NumberFormatException ignore) {
                    /* ignore */
                }
                return val;
            } else if(arguments.size() > 0 && arguments.get(0) instanceof Double) {
                return arguments.get(0);
            } else {
                return null;
            }
        });

        parser.registerFunction("min", (arguments) -> {
            Double minValue = null;
            for(Object argument : arguments) {
                if(argument instanceof Double) {
                    if(minValue == null || (Double)argument < minValue) {
                        minValue = (Double) argument;
                    }
                }
            }
            return minValue;
        });


        parser.registerFunction("abs", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) instanceof Double) {
                Double val = (Double)(arguments.get(0)) ;
                return Math.abs(val);

            }
            return null;
        });

        parser.registerFunction("ceil", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) instanceof Double) {
                Double val = (Double)(arguments.get(0)) ;
                return Math.ceil(val);

            }
            return null;
        });

        parser.registerFunction("floor", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) instanceof Double) {
                Double val = (Double)(arguments.get(0)) ;
                return Math.floor(val);

            }
            return null;
        });

    }


    /**
     * Registers the common Terraform Collection Functions as defined in the hashicorp documentation
     * Refer to: <a href="https://developer.hashicorp.com/terraform/language/functions">https://developer.hashicorp.com/terraform/language/functions</a>
     * NOTE: This is called from the main "registerBaseFunctions" automatically
     * @param parser the parser object we are registering the functions
     */
    static void registerCollectionFunctions(HCLParser parser) {
        parser.registerFunction("element", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    Double val = (Double)(arguments.get(1)) ;
                    return elements.get(val.intValue());
                } else {
                    return null;
                }
            }
            return null;
        });

        parser.registerFunction("length", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    return new Double(elements.size());
                } else {
                    return null;
                }
            }
            return null;
        });

        parser.registerFunction("index", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    Object val = arguments.get(1);
                    return elements.indexOf(val);
                } else {
                    return null;
                }
            }
            return null;
        });

        parser.registerFunction("one", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    if(elements.size() > 0) {
                        return elements.get(0);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            return null;
        });

        parser.registerFunction("alltrue", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    if(elements.size() > 0) {
                        Boolean allTrue = true;
                        for(Object element : elements) {
                            if(element instanceof Boolean) {
                                if(!(Boolean)element) {
                                    allTrue = false;
                                    break;
                                }
                            } else if(element instanceof String) {
                                if(!element.equals("true")) {
                                    allTrue = false;
                                    break;
                                }
                            } else {
                                allTrue = false;
                                break;
                            }
                        }
                        return allTrue;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            return true;
        });


        parser.registerFunction("anytrue", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    if(elements.size() > 0) {
                        for(Object element : elements) {
                            if(element instanceof Boolean) {
                                if((Boolean)element) {
                                    return true;
                                }
                            } else if(element instanceof String) {
                                if(element.equals("true")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        });

        parser.registerFunction("lookup", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof Map) {
                    Map<String,Object> elements = ((Map<String,Object>)(arguments.get(0)));
                    String key = ((String)(arguments.get(1)));
                    Object value = elements.get(key);
                    if(value == null && arguments.size() > 2) {
                       value = arguments.get(2);
                    }
                    return value;
                } else {
                    return null;
                }
            }
            return null;
        });

        parser.registerFunction("flatten", (arguments) -> {
            if(arguments.size() > 0) {
                if(arguments.get(0) instanceof List) {
                    List<Object> elements = ((List<Object>)(arguments.get(0)));
                    ArrayList<Object> flattened = new ArrayList<>();
                    flattenList(flattened,elements);

                    return flattened;
                } else {
                    return null;
                }
            }
            return null;
        });


    }

    private static void flattenList(ArrayList<Object> flattened, List<Object> elements) {
        for(Object element : elements) {
            if(element instanceof List) {
                List<Object> subElements = ((List<Object>)(element));
                flattenList(flattened,subElements);
            } else {
                flattened.add(element);
            }
        }

    }


    static void registerDateFunctions(HCLParser parser) {
        parser.registerFunction("timestamp", (arguments) -> {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                    .format(new Date());
        });
    }

    static void registerCastingFunctions(HCLParser parser) {
        parser.registerFunction("jsonencode", (arguments) -> {
            if(arguments.size() > 0) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arguments.get(0));
                } catch(JsonProcessingException ex) {
                    //SHOULD WE LOG THIS
                }
            }
            return null;
        });
        parser.registerFunction("jsondecode", (arguments) -> {
            if(arguments.size() > 0 && arguments.get(0) instanceof String) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    String val = (String)(arguments.get(0));
                    JsonNode node = objectMapper.readTree(val);
                    if(node.getNodeType() == JsonNodeType.OBJECT) {
                        Map<String, Object> result = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>(){});
                        return result;
                    } else if(node.getNodeType() == JsonNodeType.ARRAY) {
                        ArrayList<Object> result = objectMapper.convertValue(node, new TypeReference<ArrayList<Object>>(){});
                        return result;
                    }
                } catch(JsonProcessingException ex) {
                    //SHOULD WE LOG THIS
                }
            }
            return null;
        });

        parser.registerFunction("base64encode", (arguments) -> {
            if(arguments.size() > 0) {
                String content = (String)(arguments.get(0));
                if(content != null) {
                    return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
                }
            }
            return null;
        });

        parser.registerFunction("base64decode", (arguments) -> {
            if(arguments.size() > 0) {
                String content = (String)(arguments.get(0));
                if(content != null) {
                    byte[] decodedBytes = Base64.getDecoder().decode(content);
                    return new String(decodedBytes,StandardCharsets.UTF_8);
                }
                
            }
            return null;
        });


        parser.registerFunction("textencodebase64", (arguments) -> {
            if(arguments.size() > 1) {
                String content = (String)(arguments.get(0));
                String encoding = (String)(arguments.get(1));
                if(content != null) {
                    return Base64.getEncoder().encodeToString(content.getBytes(Charset.forName(encoding)));
                }
            }
            return null;
        });

        parser.registerFunction("textdecodebase64", (arguments) -> {
            if(arguments.size() > 1) {
                String content = (String)(arguments.get(0));
                if(content != null) {
                    String encoding = (String)(arguments.get(1));

                    byte[] decodedBytes = Base64.getDecoder().decode(content);
                    return new String(decodedBytes,Charset.forName(encoding));
                }
                
            }
            return null;
        });
    }


}
