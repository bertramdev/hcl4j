package com.bertramlabs.plugins.hcl4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.StringTokenizer;

public class HCLBaseFunctions {
    static void registerBaseFunctions(HCLParser parser) {
        parser.registerFunction("upper", (arguments) -> {
            if(arguments.size() > 0) {
                return arguments.get(0).toString().toUpperCase(Locale.ROOT);
            }
            return null;
        });

        parser.registerFunction("lower", (arguments) -> {
            if(arguments.size() > 0) {
                return arguments.get(0).toString().toLowerCase(Locale.ROOT);
            }
            return null;
        });

        parser.registerFunction("format", (arguments) -> {
            if(arguments.size() > 0) {
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
            if(arguments.size() > 0) {
                return arguments.get(0).toString();
            }
            return null;
        });


        parser.registerFunction("trim", (arguments) -> {
            if(arguments.size() > 0) {
                return arguments.get(0).toString();
            }
            return null;
        });


        parser.registerFunction("strrev", (arguments) -> {
            if(arguments.size() > 0) {
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

        parser.registerFunction("contains", (arguments) -> {
            if(arguments.size() == 2) {
                if(arguments.get(0) instanceof Collection) {
                    Collection list = (Collection)(arguments.get(0));
                    return list.contains(arguments.get(1));
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
                    String value = (String)(arguments.get(1));
                    ArrayList<String> elements = new ArrayList<>();
                    StringTokenizer tokenizer = new StringTokenizer(value,separator);
                    while(tokenizer.hasMoreTokens()) {
                        elements.add(tokenizer.nextToken());
                    }
                    return elements;
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
                return value.replace(substring,replacement);
            } else {
                return null; //Invalid Function Spec
            }
        });

        parser.registerFunction("uuid", (arguments) -> {
            return java.util.UUID.randomUUID().toString();
        });

        registerNumericFunctions(parser);
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


}
