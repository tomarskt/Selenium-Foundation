package com.nordstrom.automation.selenium.interfaces;

import org.openqa.selenium.SearchContext;

import com.google.common.base.Function;
import com.nordstrom.automation.selenium.exceptions.PageNotLoadedException;
import com.nordstrom.automation.selenium.support.Coordinator;

/**
 * Page classes that model pages with complex loading behavior implement this interface to provide scenario-specific
 * detection of page load completion. This is typically required for single-page applications or more conventional
 * multi-page applications that use dynamic load techniques (e.g. - AJAX).
 */
public interface DetectsLoadCompletion {
    
    /**
     * Determine if the page has finished loading.
     * 
     * @return 'true' if the page has finished loading; otherwise 'false'
     */
    boolean isLoadComplete();
    
    /**
     * Returns a 'wait' proxy that determines if the page has finished loading.
     * 
     * @return 'true' if the page has finished loading; otherwise 'false'
     */
    static Coordinator<Boolean> pageLoadIsComplete() {
        return new Coordinator<Boolean>() {

            @Override
            public Boolean apply(final SearchContext context) {
                if (context instanceof DetectsLoadCompletion) {
                    return Boolean.valueOf(((DetectsLoadCompletion) context).isLoadComplete());
                }
                throw new IllegalArgumentException(String.format("Search context type '%s' doesn't implement the "
                                + "DetectsLoadCompletion interface", context.getClass().getName()));
            }
            
            @Override
            public String toString() {
                return "page to finish loading";
            }
        };
    }

    /**
     * Check the specified page-load condition to determine if this condition has been met.<br>
     * NOTE - This method indicates failure to meet the condition by throwing {@link PageNotLoadedException}.
     * 
     * @param <T> coordinator type parameter
     * @param condition expected page-load condition
     * @param message the detail message for the {@link PageNotLoadedException} thrown if the condition isn't met
     * @return result from the {@link Function#apply(Object) apply} method of the specified coordinator
     */
    default <T> T checkPageLoadCondition(final Coordinator<T> condition, final String message) {
        T result = null;
        Throwable cause = null;
        try {
            result = condition.apply(getContext());
        } catch (RuntimeException t) {
            cause = t;
        }
        if (cause != null) {
            throw new PageNotLoadedException(message, cause);
        } else if (result == null || result == Boolean.FALSE) {
            throw new PageNotLoadedException(message);
        }
        return result;
    }
    
    /**
     * Get the container search context
     * 
     * @return container search context
     */
    SearchContext getContext();
    
}
