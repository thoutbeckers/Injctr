package houtbecke.rs.injctr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.apache.commons.lang3.StringUtils;

import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import houtbecke.rs.injctr.base.InjctrView;

public class InjctrUtil {

    Context context;
    Resources resources;

    @Inject
    public InjctrUtil(Context c, Resources resources) {
        this.context = c;
        this.resources = resources;
    }

    public String getFragmentTitle(String prefix, Fragment fragment) {
        String fragmentTitle = "";

        if (fragment.getClass().isAnnotationPresent(Title.class)) {
            Title title = fragment.getClass().getAnnotation(Title.class);
            fragmentTitle = title.string();
            if ("".equals(fragmentTitle) && title.value() != 0)
                fragmentTitle = resources.getString(title.value());
            else if ("".equals(fragmentTitle) && title.value() == 0) {
                int res = getResourceIdentifier(prefix+" "+fragment.getClass().getSimpleName(), "string");
                if (res != 0)
                    return resources.getString(res);
            }
        }
        return fragmentTitle;
    }


    public String getFragmentTitle(Fragment fragment) {
        String fragmentTitle = "";

        if (fragment.getClass().isAnnotationPresent(Title.class)) {
            Title title = fragment.getClass().getAnnotation(Title.class);
            fragmentTitle = title.string();
            if ("".equals(fragmentTitle) && title.value() != 0)
                fragmentTitle = resources.getString(title.value());
        }
        return fragmentTitle;
    }

    public int getFragmentLevel(Fragment fragment) {
        int fragmentLevel = 0;
        if (fragment.getClass().isAnnotationPresent(Level.class)) {
            Level level = fragment.getClass().getAnnotation(Level.class);
            fragmentLevel = level.value();
        }
        return fragmentLevel;
    }

    public int getFragmentImage(Fragment fragment) {
        int fragmentImage = -1;
        if (fragment.getClass().isAnnotationPresent(Image.class)) {
            Image image = fragment.getClass().getAnnotation(Image.class);
            fragmentImage = image.value();

        }
        return fragmentImage;
    }

    public int getFragmentLayout(Fragment fragment) {
        return getLayout(fragment, "fragment");
        // maybe include a getter in Base object for dynamic titles
    }

    public int getViewLayout(View v) {
        return getLayout(v, "view");
    }

    public static String constructName(String type, Object name) {
        String simpleName = name.getClass().getSimpleName();
        if (simpleName.toLowerCase(Locale.ENGLISH).endsWith(type))
            simpleName = simpleName.substring(0, simpleName.length() - type.length());
        return getUnderscoredString(type, simpleName);
    }

    private static String getUnderscoredString(String type, String simpleName) {
        return getUnderscoredString(new StringBuilder(type), simpleName).toString();
    }

    private String getUnderscoredString(String simpleName) {
        return getUnderscoredString(new StringBuilder(), simpleName).toString();
    }

    private static StringBuilder getUnderscoredString(StringBuilder builder, String simpleName) {
        for (String part: StringUtils.splitByCharacterTypeCamelCase(simpleName)) {
            if (builder.length() > 0)
                builder.append("_");
            builder.append(part.toLowerCase());
        }
        return builder;
    }


    public int getId(String name) {
        return getResourceIdentifier(getUnderscoredString(name), "id");
    }

    public int getResourceIdentifier(String name, String type) {
        return getResourceIdentifier(context, resources, name, type);
    }

    public static int getResourceIdentifier(Context context, Resources resources, String name, String type) {
        return resources.getIdentifier(name, type, context.getPackageName()) ;
    }

    public void injctrActivity(Activity activity) {
        injctr(activity, activity, null, activity.getWindow().getDecorView());
    }

    @TargetApi(11)
    public void injctrFragment(Fragment fragment) {
        injctr(fragment.getActivity(), fragment, null, fragment.getView());
    }

    public void injctrView(View view, Context styledContext, AttributeSet attrs) {
        injctr(styledContext, view, attrs, view);
    }

    public void injctrRes(Object injctrObject) {
        injctr(context, injctrObject, null, null);

    }

    public void injctr(Context styledContext, Object injctrObject, AttributeSet attrs, View rootView) {

        // Styleables can differ per class so they need a second layer in the hierarchy
        Map<StyleableInfo, Map<Integer, Field>> styleables = attrs == null ? null : new LinkedHashMap<StyleableInfo, Map<Integer, Field>>();

        Class clazz = injctrObject.getClass();
        while (clazz != InjctrView.class && clazz != null) {
            StyleableInfo styleableInfo;
            if (clazz.isAnnotationPresent(Styleable.class)) {
                Styleable styleableAnnotation = (Styleable) clazz.getAnnotation(Styleable.class);
                if ("".equals(styleableAnnotation.packageName()))
                    styleableInfo = getStyleable(styleableAnnotation.value());
                else
                    styleableInfo = getStyleable(styleableAnnotation.packageName(), styleableAnnotation.value());
            }
            else
                styleableInfo = getStyleable(clazz.getSimpleName());

            Map<Integer, Field> styleableFieldMap = null;

            for (Field field: clazz.getDeclaredFields()) {
                int styleableId = -2;
                Class fieldClass = field.getType();
                Annotation[] annotations = field.getDeclaredAnnotations();
                for (Annotation a: annotations) {
                    Class anonClass = a.annotationType();
                    if (rootView != null && anonClass == houtbecke.rs.injctr.View.class && android.view.View.class.isAssignableFrom(fieldClass)) {
                        houtbecke.rs.injctr.View viewAnon = (houtbecke.rs.injctr.View) a;
                        View view = rootView;
                        for (int parent : viewAnon.parents())
                            view = view.findViewById(parent);
                        int viewId = viewAnon.value();
                        if (viewId == -1)
                            viewId = getId(field.getName());
                        view = view.findViewById(viewId);
                        setField(field, injctrObject, view);

                    } else if (anonClass == ResString.class) {
                        int id = ((ResString)a).value();
                        id = id != -1 ? id : getResourceIdentifier(getUnderscoredString(field.getName()), "string");
                        setField(field, injctrObject, resources.getString(id));
                    } else if (styleables == null || styleableInfo == null || styleableInfo.styleable == null) {
                        // do nothing
                    }
                    else if (anonClass == Attr.class)
                        styleableId = field.getAnnotation(Attr.class).value();
                    else if (anonClass == AttrColor.class)
                        styleableId = field.getAnnotation(AttrColor.class).value();
                    else if (anonClass == AttrDimen.class)
                        styleableId = field.getAnnotation(AttrDimen.class).value();
                    else if (anonClass == AttrColor.class)
                        styleableId = field.getAnnotation(AttrColor.class).value();
                    else if (anonClass == AttrDimenOffset.class)
                        styleableId = field.getAnnotation(AttrDimenOffset.class).value();
                    else if (anonClass == AttrDimenSize.class)
                        styleableId = field.getAnnotation(AttrDimenSize.class).value();
                    else if (anonClass == AttrFraction.class)
                        styleableId = field.getAnnotation(AttrFraction.class).value();
                    else if (anonClass == AttrLayoutDimen.class)
                        styleableId = field.getAnnotation(AttrLayoutDimen.class).value();
                    else if (anonClass == AttrNoResString.class)
                        styleableId = field.getAnnotation(AttrNoResString.class).value();
                    else if (anonClass == AttrPosDesc.class)
                        styleableId = field.getAnnotation(AttrPosDesc.class).value();
                    else if (anonClass == AttrResId.class)
                        styleableId = field.getAnnotation(AttrResId.class).value();
                    else if (anonClass == AttrString.class)
                        styleableId = field.getAnnotation(AttrString.class).value();
                    else if (anonClass == AttrText.class)
                        styleableId = field.getAnnotation(AttrText.class).value();


                    else if (anonClass == ResString.class) {
                        int id = ((ResString)a).value();
                        id = id != -1 ? id : getResourceIdentifier(getUnderscoredString(field.getName()), "string");
                        setField(field, injctrObject, resources.getString(id));
                    }

                    if (styleableId == -1)
                        styleableId = styleableInfo.attribute(field.getName());
                }

                if (styleableId >= 0) {
                    if (styleableFieldMap == null)
                        styleableFieldMap = new HashMap<>();
                    styleableFieldMap.put(styleableId, field);
                }
            }

            if (styleableFieldMap != null)
                styleables.put(styleableInfo, styleableFieldMap);

            clazz = clazz.getSuperclass();
        }

        if (styleables == null)
            return;

        for (StyleableInfo styleable: styleables.keySet()) {
            TypedArray array = styledContext.obtainStyledAttributes(attrs, styleable.styleable);
            Map<Integer, Field> styleableFieldMap = styleables.get(styleable);
            for (int k = 0; k < array.getIndexCount(); k++) {
                int attribute = array.getIndex(k);
                Field field = styleableFieldMap.get(attribute);
                if (field != null) {
                    Class type = field.getType();
                    Annotation[] annotations = field.getAnnotations();
                    for (Annotation annotation: annotations) {
                        Class aType = annotation.annotationType();
                        boolean fieldAccessible = field.isAccessible();
                        try {
                            if (!fieldAccessible)
                                field.setAccessible(true);
                            if (aType == Attr.class) {
                                if (type == int.class)
                                    field.setInt(injctrObject, array.getInt(attribute, field.getInt(injctrObject)));
                                else if (type == float.class)
                                    field.setFloat(injctrObject, array.getFloat(attribute, field.getFloat(injctrObject)));
                                else if (type == boolean.class)
                                    field.setBoolean(injctrObject, array.getBoolean(attribute, field.getBoolean(injctrObject)));
                                else if (type.isAssignableFrom(ColorStateList.class))
                                    field.set(injctrObject, array.getColorStateList(attribute));
                                else if (type.isAssignableFrom(Drawable.class)) {
                                    field.set(injctrObject, array.getDrawable(attribute));
                                }
                                break;
                            } else if (aType == AttrColor.class) {
                                if (type == int.class)
                                    field.setInt(injctrObject, array.getColor(attribute, field.getInt(injctrObject)));
                                else if (type.isAssignableFrom(ColorStateList.class))
                                    field.set(injctrObject, array.getColorStateList(attribute));
                                break;
                            } else if (aType == AttrDimen.class && type == float.class) {
                                field.setFloat(injctrObject, array.getDimension(attribute, field.getFloat(injctrObject)));
                                break;
                            } else if (aType == AttrDimenOffset.class && type == int.class) {
                                field.setInt(injctrObject, array.getDimensionPixelOffset(attribute, field.getInt(injctrObject)));
                                break;
                            } else if (aType == AttrDimenSize.class && type == int.class) {
                                field.setInt(injctrObject, array.getDimensionPixelSize(attribute, field.getInt(injctrObject)));
                                break;
                            } else if (aType == AttrFraction.class && type == float.class) {
                                AttrFraction attrFraction = (AttrFraction) annotation;
                                field.setFloat(injctrObject, array.getFraction(attribute, attrFraction.base(), attrFraction.pbase(), field.getFloat(injctrObject)));
                                break;
                            } else if (aType == AttrLayoutDimen.class && type == int.class) {
                                AttrLayoutDimen attrLayoutDimen = (AttrLayoutDimen) annotation;
                                if (!"".equals(attrLayoutDimen.name()))
                                    field.setInt(injctrObject, array.getLayoutDimension(attribute, attrLayoutDimen.name()));
                                else
                                    field.setInt(injctrObject, array.getLayoutDimension(attribute, field.getInt(injctrObject)));
                                break;
                            } else if (aType == AttrNoResString.class && type == String.class) {
                                field.set(injctrObject, array.getNonResourceString(attribute));
                                break;
                            } else if (aType == AttrPosDesc.class && type == String.class) {
                                field.set(injctrObject, array.getNonResourceString(attribute));
                                break;
                            } else if (aType == AttrResId.class && type == int.class) {
                                field.setInt(injctrObject, array.getResourceId(attribute, field.getInt(injctrObject)));
                                break;
                            } else if (aType == AttrString.class && type == String.class) {
                                field.set(injctrObject, array.getString(attribute));
                                break;
                            } else if (aType == AttrText.class) {
                                if (type.isAssignableFrom(CharSequence.class))
                                    field.set(injctrObject, array.getText(attribute));
                                else if (type.isAssignableFrom(CharSequence[].class))
                                    field.set(injctrObject, array.getTextArray(attribute));
                                break;
                            }

                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            // too bad, no access to this field. Should be rare enough not to handle. u mad?
                        } finally {
                            if (!fieldAccessible)
                                field.setAccessible(false);
                        }
                        if (annotation == annotations[annotations.length -1])
                            Log.w("Attr", "Trying to fill field annotated with" + aType.getSimpleName() +
                                    "but " + type.getSimpleName() + " is not compatible");
                    }
                }

            }
        }
    }

    private void setField(Field field, Object object, Object value) {
        boolean changed = false;
        if (!field.isAccessible())
            field.setAccessible(changed = true);
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            if (changed)
                field.setAccessible(false);
        }
    }

    public static class StyleableInfo {
        public int[] styleable;
        private Map<String, Integer> nameValueMap = new HashMap<>();
        public int attribute(String name) {
            Integer ret = nameValueMap.get(name);
            return ret != null ? ret: -1; // who else miss the king ?:
        }
    }

    public final StyleableInfo getStyleable(String name) {
        return getStyleable(context.getPackageName(), name);
    }

    static Map<String, Field[]> styleableFieldsCache = new HashMap<>();
    static Map<String, StyleableInfo> styleableInfoCache = new HashMap<>();

    public final StyleableInfo getStyleable(String packageName, String name) {
        final String styleableInfoKey = packageName+"."+name;
        if (styleableInfoCache.containsKey(styleableInfoKey))
            return styleableInfoCache.get(styleableInfoKey);

        try {
            Field[] fields;
            String className;
            try {
                className =  packageName + ".R$styleable";
                fields = styleableFieldsCache.get(className);
                if (fields == null) fields = Class.forName(className).getFields();

            } catch (ClassNotFoundException cnf) {
                // it's possible that we are in a packageNameSuffix build, which seems to still build styleable under the regular package :$
                packageName = packageName.substring(0, packageName.lastIndexOf('.'));
                className = packageName+ ".R$styleable";
                fields = styleableFieldsCache.get(className);
                if (fields == null) fields = Class.forName(className).getFields();
            }

            styleableFieldsCache.put(className, fields);

            StyleableInfo styleableInfo = new StyleableInfo();
            for (Field field : fields) {
                String fieldName = field.getName();
                if (fieldName.startsWith(name)) {

                    if (fieldName.equals(name))
                        styleableInfo.styleable = (int[])field.get(null);
                    else {
                        int val = field.getType().isArray() ?  ((int[]) field.get(null))[0] : field.getInt(null);
                        styleableInfo.nameValueMap.put(fieldName.substring(name.length() + 1), val);
                    }
                }
            }
            styleableInfoCache.put(styleableInfoKey, styleableInfo);
            return styleableInfo;
        } catch (IllegalAccessException ignore) {
            // at this point we just assume reflection has failed us to find the styleables.
        } catch (ClassNotFoundException ignore) {
        }
        styleableInfoCache.put(styleableInfoKey, null);
        return null;
    }

    public int getLayout(Object object, String type) {
        return getLayout(context, resources, object, type);
    }

    public static int getLayout(Context context, Resources resources, Object object, String type) {
        if (object.getClass().isAnnotationPresent(Layout.class)) {
            Layout layout = object.getClass().getAnnotation(Layout.class);
            if (layout.value() != 0)
                return layout.value();

        }
        String name = constructName(type, object);
        return getResourceIdentifier(context, resources, name, "layout");
    }

    public int getStringId(String type, Object object, String... postFixes) {
        String name = constructName(type, object);
        for (String postfix: postFixes)
            name+="_"+postfix;
        return getResourceIdentifier(name, "string");
    }



}
