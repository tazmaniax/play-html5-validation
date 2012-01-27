/* Project: play-html5-validation
 * Package: tags.html5validation
 * File   : HTML5ValidationTags
 * Created: Dec 5, 2010 - 7:27:42 PM
 *
 *
 * Copyright 2010 Sebastian Hoß
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
 *
 */
package tags.html5validation;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import play.Play;
import play.data.validation.Email;
import play.data.validation.Match;
import play.data.validation.Max;
import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.Password;
import play.data.validation.Range;
import play.data.validation.Required;
import play.data.validation.URL;
import play.db.Model;
import play.exceptions.TemplateCompilationException;
import play.mvc.Scope.RenderArgs;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

/**
 * <h1>Overview</h1>
 * <p>The HTML5 validation tags provide a simple <code>#{html5.input /}</code> tag which can be used as a drop-in
 * replacement for existing HTML5 <code>&lt;input&gt;</code> elements.</p>
 *
 * <p>The <code>#{html5.input /}</code> tag will try to map existing data validation annotations from your Play! model
 * to the HTML5 input element and thus provide near codeless client-side validation without using JavaScript.</p>
 *
 * <p>On top of that it supports all available attributes from the original HTML5 input element and auto-
 * fills the <code>name</code> attribute by default.</p>
 *
 * <p>For that to work you have to specify the model instance and its field you want to map by using the
 * <em>for</em> attribute:<br>
 * <br>
 * <code>#{html5.input for:'user.name' /}</code></p>
 * 
 * <h1>Caveats</h1>
 * <ul>
 *  <li>The MinSize validator can not be mapped to any HTML5 attribute currently.</li>
 *  <li>Contrary to HTML5 input elements the <code>#{html5.input /}</code> tag must be properly closed.</li>
 *  <li>Attributes specified in the tag have priority over the validation annotations.</li>
 *  <li>There is no checking to ensure that attributes are valid for the input type - if the annotation exists the attribute is written</li>
 * </ul>
 *
 * <h1>Examples</h1>
 * <ol>
 *  <li>
 *      <p>Username validation</p>
 *      <p>Suppose you have a {@link Model} called <code>User</code> which has a field called
 *      <code>name</code> declared as</p><br>
 *
 *      <p><code>@Required<br>
 *      @MaxSize(8)<br>
 *      public String name;</code></p><br>
 *
 *      <p>and you pass an instance of that class called <em>user</em> around, you then can
 *      specify the field from that instance inside the <code>#{html5.input /}</code> as follows:</p>
 *
 *      <p><code>#{html5.input for:'user.name', id:'yourID', class:'class1 clas2' /}</code></p><br>
 *
 *      <p>The tag will then output the following HTML code:</p><br>
 *
 *      <p><code>&lt;input name="user.name" value="${user?.name}" id="yourID" class="class1 class2" required
 *      maxlength="8"&gt;</code></p><br>
 *  </li>
 * </ol>
 *
 * <h1>How to help</h1>
 * <ul>
 *  <li>Test the tag and write back about errors, bugs and wishes.</li>
 * </ul>
 *
 * @author  Sebastian Hoß (mail@shoss.de)
 * @author  Chris Webb (chris@spinthewebb.com)
 * @see     <a href="http://www.w3.org/TR/html-markup/input.html">HTML5 Input Element</a>
 * @see     <a href="http://www.playframework.org/documentation/1.2.4/validation-builtin">Built-in
  				validations by Play!</a>
 * @see     <a href="http://diveintohtml5.org/forms.html">Mark Pilgrim on HTML5 Forms</a>
 */
@SuppressWarnings("nls")
@FastTags.Namespace("html5")
public final class HTML5ValidationTags extends FastTags {

    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // *                                             METHODS                                             *
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *

    /**
     * <p>Generates an HTML5 <code>&lt;input&gt;</code> element with some values derived from a given field.</p>
     * 
     * @param args      The tag attributes.
     * @param body      The tag body.
     * @param out       The print writer to use.
     * @param template  The parent template.
     * @param fromLine  The current execution line.
     */
    public static void _input(final Map<?, ?> args, final Closure body, final PrintWriter out,
            final ExecutableTemplate template, final int fromLine) {
        try {
            // Open input tag
            out.print("<input");
            
            Map<String, String> renderArgs = new HashMap<String, String>();
            // Copy all arguments to render arguments, except for 'for' argument.
            for (final Object attribute : args.keySet()) {
                if (!"for".equalsIgnoreCase(attribute.toString()) && (args.get(attribute) != null)) {
                	renderArgs.put(attribute.toString(), args.get(attribute).toString());
                }
            }
            
            final String fieldname = args.get("for").toString();
            final String[] pieces = fieldname.split("\\.");
            Object obj = template.getProperty(pieces[0]);   

            Field field = null;
            Object value = null;
            
			if (obj != null) {
				if (pieces.length > 1) {
					for (int i = 1; i < pieces.length; i++) {
						try {
							Field f = obj.getClass().getField(pieces[i]);
							if (i == (pieces.length - 1)) {
								field = f;
								try {
									Method getter = obj.getClass().getMethod("get" + JavaExtensions.capFirst(f.getName()));
									value = getter.invoke(obj, new Object[0]);
								} catch (NoSuchMethodException e) {
									value = f.get(obj).toString();
								}
							} else {
								obj = f.get(obj);
							}
						} catch (Exception e) {
							// if there is a problem reading the field we don't set any value
						}
					}
				} else {
					value = obj;
				}
			}
			
			if (fieldname != null && renderArgs.get("name") == null) {
				renderArgs.put("name", fieldname);
			}
			
			if (value != null && renderArgs.get("value") == null) {
				renderArgs.put("value", value.toString());
			}
			
			if (field != null) {
		        // Mark readonly
		        if (Modifier.isFinal(field.getModifiers()) || args.containsKey("readonly")) {
		        	renderArgs.put("readonly", "readonly");
		        }

		        // Print the validation data
		        if (field.isAnnotationPresent(Required.class)) {
		        	renderArgs.put("required", "required");
		        }

		        if (field.isAnnotationPresent(Min.class) && renderArgs.get("min") == null) {
		            final Min min = field.getAnnotation(Min.class);
		            renderArgs.put("min", String.valueOf(min.value()));
		        }

		        if (field.isAnnotationPresent(Max.class) && renderArgs.get("max") == null) {
		            final Max max = field.getAnnotation(Max.class);
		            renderArgs.put("max", String.valueOf(max.value()));
		        }

		        if (field.isAnnotationPresent(Range.class)) {
		            final Range range = field.getAnnotation(Range.class);
		            if (renderArgs.get("min") == null) {
		            	renderArgs.put("min", String.valueOf(range.min()));
		            }
		            
		            if (renderArgs.get("max") == null) {
		            	renderArgs.put("max", String.valueOf(range.max()));
		            }
		        }

		        if (field.isAnnotationPresent(MaxSize.class) && renderArgs.get("maxlength") == null) {
		            final MaxSize maxSize = field.getAnnotation(MaxSize.class);
		            renderArgs.put("maxlength", String.valueOf(maxSize.value()));
		        }

		        if (field.isAnnotationPresent(Match.class) && renderArgs.get("pattern") == null) {
		            final Match match = field.getAnnotation(Match.class);
		            renderArgs.put("pattern", match.value());
		        }

		        if (renderArgs.get("type") == null) {
			        if (field.isAnnotationPresent(URL.class)) {
			        	renderArgs.put("type", "url");
			        } else if (field.isAnnotationPresent(Email.class)) {
			        	renderArgs.put("type", "email");
			        } else if (field.isAnnotationPresent(Password.class)) {
			        	renderArgs.put("type", "password");
			        } else if (CharSequence.class.isAssignableFrom(field.getType())) {
			        	renderArgs.put("type", "text");
			        } else if (Number.class.isAssignableFrom(field.getType())) {
			        	renderArgs.put("type", "number");
			        }
		        }
			}

	        for (final Object attrKey : renderArgs.keySet()) {
	            if (renderArgs.get(attrKey) != null) {
	                printAttribute(attrKey.toString(), renderArgs.get(attrKey).toString(), out);
	            }
	        }

            // Close input tag
            out.println(">");

        } catch (final SecurityException exception) {
            throw new TemplateCompilationException(template.template, Integer.valueOf(fromLine), exception.getMessage());
        } catch (final IllegalArgumentException exception) {
            throw new TemplateCompilationException(template.template, Integer.valueOf(fromLine), exception.getMessage());
        }
    }

    /**
     * <p>Prints a single attribute using a given print writer.</p>
     * 
     * <p>If <code>null</code> is given as value nothing will be printed to eliminate empty attributes.</p>
     *
     * @param name      The name of the attribute to print.
     * @param value     The value of the attribute to print (may be <code>null</code>).
     * @param out       The print writer to use.
     */
    private static void printAttribute(final String name, final Object value, final PrintWriter out) {
        if (value != null) {
            out.print(" " + name + "=\"" + value + "\"");
        }
    }

}
