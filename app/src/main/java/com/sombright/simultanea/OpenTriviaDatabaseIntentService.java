package com.sombright.simultanea;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class OpenTriviaDatabaseIntentService extends IntentService {
    private static final String TAG = "OpenTDBService";

    private static final String ACTION_FETCH_NEW_ITEMS = "com.sombright.simultanea.action.FETCH_NEW_ITEMS";
    private static final String ACTION_FETCH_CATEGORIES = "com.sombright.simultanea.action.FETCH_CATEGORIES";

    private static final String EXTRA_AMOUNT = "com.sombright.simultanea.extra.AMOUNT";
    private static final String EXTRA_CATEGORY = "com.sombright.simultanea.extra.CATEGORY";
    private static final String EXTRA_DIFFICULTY = "com.sombright.simultanea.extra.DIFFICULTY";
    private static final String EXTRA_TYPE = "com.sombright.simultanea.extra.TYPE";

    // API URL
    private static final String API_URL = "https://opentdb.com/api.php";
    private static final String AMOUNT_KEY = "amount";
    private static final String CATEGORY_KEY = "category";
    private static final int DEFAULT_CATEGORY = 9;
    private static final String DIFFICULTY_KEY = "difficulty";
    private static final String TYPE_KEY = "type";
    private static final String TYPE_MULTIPLE_CHOICE = "multiple";
    private static final String TYPE_TRUE_OR_FALSE = "boolean";

    // Session Tokens
    private static final long SESSION_TOKEN_IDLE_TIMEOUT_MS = 6*60*60*1000;
    private static final String SESSION_TOKEN_REQUEST_URL = "https://opentdb.com/api_token.php?command=request";
    private static final String SESSION_TOKEN_RESET_URL = "https://opentdb.com/api_token.php?command=reset&token=%s";

    // Encoding Types
    private static final String ENCODING_DEFAULT = "";
    private static final String ENCODING_LEGACY_URL = "urlLegacy";
    private static final String ENCODING_URL = "url3986";
    private static final String ENCODING_BASE64 = "base64";

    // Helper API Tools

    // Category Lookup: Returns the entire list of categories and ids in the database.
    private static final String CATEGORY_LOOKUP_URL = "https://opentdb.com/api_category.php";
    // Category Question Count Lookup: Returns the number of questions in the database, in a specific category.
    private static final String CATEGORY_QUESTION_COUNT_LOOKUP_URL = "https://opentdb.com/api_count.php?category=%s";
    // Global Question Count Lookup: Returns the number of ALL questions in the database. Total, Pending, Verified, and Rejected.
    private static final String GLOBAL_QUESTION_COUNT_LOOKUP_URL = "https://opentdb.com/api_count_global.php";

    // Limitations
    // Only 1 Category can be requested per API Call. To get questions from any category, don't specify a category.

    // A Maximum of 50 Questions can be retrieved per call.
    private static final int MIN_AMOUNT = 1;
    private static final int MAX_AMOUNT = 50;

    // Instantiate the RequestQueue.
    private RequestQueue requestQueue;

    public OpenTriviaDatabaseIntentService() {
        super("OpenTriviaDatabaseIntentService");
        // Instantiate the RequestQueue.
        requestQueue = Volley.newRequestQueue(this);
    }

    /**
     * Starts this service to perform action FetchNewItems with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchNewItems(Context context, int amount, int category, String difficulty, int type) {
        Intent intent = new Intent(context, OpenTriviaDatabaseIntentService.class);
        intent.setAction(ACTION_FETCH_NEW_ITEMS);
        if (amount < MIN_AMOUNT)
            amount = MIN_AMOUNT;
        else if (amount > MAX_AMOUNT)
            amount = MAX_AMOUNT;
        intent.putExtra(EXTRA_AMOUNT, amount);
        if (category >= 0)
            intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_DIFFICULTY, difficulty);
        intent.putExtra(EXTRA_TYPE, type);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action FetchCategories. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchCategories(Context context) {
        Intent intent = new Intent(context, OpenTriviaDatabaseIntentService.class);
        intent.setAction(ACTION_FETCH_CATEGORIES);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_NEW_ITEMS.equals(action)) {
                final int amount = intent.getIntExtra(EXTRA_AMOUNT, 10);
                final int category = intent.getIntExtra(EXTRA_CATEGORY, DEFAULT_CATEGORY);
                final String difficulty = intent.getStringExtra(EXTRA_DIFFICULTY);
                final String type = intent.getStringExtra(EXTRA_TYPE);
                handleActionFetchNewItems(amount, category, difficulty, type);
            } else if (ACTION_FETCH_CATEGORIES.equals(action)) {
                handleActionFetchCategories();
            }
        }
    }

    /**
     * Handle action FetchNewItems in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetchNewItems(int amount, int category, String difficulty, String type) {
        // TODO: Handle action FetchNewItems
        String url = API_URL + "?" +
                AMOUNT_KEY + "=" + amount;
        if (category >= 0) {
            url += "&" + CATEGORY_KEY + "=" + category;
        }
        if (!difficulty.equals("")) {
            url += "&" + DIFFICULTY_KEY + "=" + difficulty;
        }
        if (!type.equals("")) {
            url += "&" + TYPE_KEY + "=" + type;
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action FetchCategories in the provided background thread.
     */
    private void handleActionFetchCategories() {
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, CATEGORY_LOOKUP_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d(TAG, "Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "That didn't work!");
                    }
                });
        // Add the request to the RequestQueue.
        requestQueue.add(stringRequest);
    }
}
