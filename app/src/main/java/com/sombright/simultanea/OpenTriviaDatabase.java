package com.sombright.simultanea;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ArrayAdapter;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.FROM_HTML_MODE_LEGACY;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The Open Trivia Database
 * https://opentdb.com/api_config.php
 */
class OpenTriviaDatabase {
    private static final String TAG = "opentdb";
    private final Context mContext;
    private Handler mHandler = new Handler();
    private RequestQueue requestQueue;
    private String mSessionToken;
    private Listener mListener;
    private List<Question> mQuestions = new ArrayList<>();
    private static final int RETRY_DELAY_MS = 10000;

    // API URL
    private static final String API_SCHEME = "https";
    private static final String API_AUTHORITY = "opentdb.com";
    private static final String API_PATH = "api.php";

    private static final String AMOUNT_KEY = "amount";
    private QuestionCount mQuestionCount = new QuestionCount();

    private static final String CATEGORY_KEY = "category";
    public static final int CATEGORY_ANY = -1;
    private int mCategory = CATEGORY_ANY;
    private SparseArray<String> mCategories = new SparseArray<>();

    public static final int DIFFICULTY_ANY = 0;
    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_MEDIUM = 2;
    public static final int DIFFICULTY_HARD = 3;
    private static final String DIFFICULTY_KEY = "difficulty";
    private static final String DIFFICULTY_VALUES[] = {"", "easy", "medium", "hard"};
    private int mDifficulty = DIFFICULTY_ANY;

    public static final int TYPE_ANY = 0;
    public static final int TYPE_MULTIPLE_CHOICE = 1;
    public static final int TYPE_TRUE_OR_FALSE = 2;
    private static final String TYPE_KEY = "type";
    private static String TYPE_VALUES[] = {"", "multiple", "boolean"};
    private int mQuestionType = TYPE_ANY;

    // Session Tokens
    private static final String TOKEN_KEY = "token";
    private static final long SESSION_TOKEN_IDLE_TIMEOUT_MS = 6 * 60 * 60 * 1000;
    private static final String SESSION_TOKEN_REQUEST_URL = "https://opentdb.com/api_token.php?command=request";
    private static final String SESSION_TOKEN_RESET_URL = "https://opentdb.com/api_token.php?command=reset&token=%s";

    // Response Codes
    private static final int RESPONSE_CODE_SUCCESS = 0; // Returned results successfully.
    private static final int RESPONSE_CODE_NO_RESULT = 1; // Could not return results. The API doesn't have enough questions for your query. (Ex. Asking for 50 Questions in a Category that only has 20.)
    private static final int RESPONSE_CODE_INVALID_PARAMETER = 2; // Contains an invalid parameter. Arguments passed in aren't valid. (Ex. Amount = Five)
    private static final int RESPONSE_CODE_TOKEN_NOT_FOUND = 3; // Session Token does not exist.
    private static final int RESPONSE_CODE_TOKEN_EMPTY = 4; // Session Token has returned all possible questions for the specified query. Resetting the Token is necessary.

    // Encoding Types
    static final int ENCODING_DEFAULT = -1;
    static final int ENCODING_LEGACY_URL = 0;
    static final int ENCODING_URL = 1;
    static final int ENCODING_BASE64 = 2;
    private static final String ENCODING_KEY = "type";
    private static final String ENCODING_VALUES[] = {
            "urlLegacy",
            "url3986",
            "base64"
    };
    private static final String ENCODING_TEST_STRING = "Don't forget that π = 3.14 & doesn't equal 3.";
    private static final String ENCODING_TEST_DATA[] = {
            "Don&‌#039;t forget that &‌pi; = 3.14 &‌amp; doesn&‌#039;t equal 3.",
            "Don%27t+forget+that+%CF%80+%3D+3.14+%26+doesn%27t+equal+3.",
            "Don%27t%20forget%20that%20%CF%80%20%3D%203.14%20%26%20doesn%27t%20equal%203.",
            "RG9uJ3QgZm9yZ2V0IHRoYXQgz4AgPSAzLjE0ICYgZG9lc24ndCBlcXVhbCAzLg=="
    };
    private int mEncodingType = ENCODING_DEFAULT;
    void setEncodingType(int encodingType) {
        switch (encodingType) {
            case ENCODING_LEGACY_URL:
            case ENCODING_URL:
            case ENCODING_BASE64:
                break;
            default:
                encodingType = ENCODING_DEFAULT;
        }
        if (encodingType != mEncodingType) {
            mEncodingType = encodingType;
        }
    }

    String decode(String encoded) {
        String decoded;
        switch (mEncodingType) {
            case ENCODING_LEGACY_URL:
            case ENCODING_URL:
                try {
                    decoded = URLDecoder.decode(encoded, UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    decoded = "";
                }
                break;
            case ENCODING_BASE64:
                decoded = new String(Base64.decode(encoded, Base64.DEFAULT));
                break;
            case ENCODING_DEFAULT:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    decoded = String.valueOf(Html.fromHtml(encoded, FROM_HTML_MODE_COMPACT));
                } else {
                    decoded = String.valueOf(Html.fromHtml(encoded));
                }
                break;
        }
        return decoded;
    }

    // Helper API Tools

    // Category Lookup: Returns the entire list of categories and ids in the database.
    private static final String CATEGORY_LOOKUP_URL = "https://opentdb.com/api_category.php";
    // Category Question Count Lookup: Returns the number of questions in the database, in a specific category.
    private static final String CATEGORY_QUESTION_COUNT_LOOKUP_URL = "api_count.php";
    // Global Question Count Lookup: Returns the number of ALL questions in the database. Total, Pending, Verified, and Rejected.
    private static final String GLOBAL_QUESTION_COUNT_LOOKUP_URL = "api_count_global.php";

    OpenTriviaDatabase(Context context) {
        mContext = context;
        requestQueue = Volley.newRequestQueue(mContext);
        fetchQuestionCount();
        getSessionToken();
        fetchCategories();
    }

    void flush() {
        mQuestionCount.reset();
        mQuestions.clear();
        if (mListener != null) {
            mListener.onQuestionsAvailable(false);
        }
    }

    void setQuestionAttributes(int category, int difficulty, int type) {
        int categoryId;
        if (category >= 0)
            categoryId = mCategories.keyAt(category);
        else
            categoryId = CATEGORY_ANY;

        if (categoryId != mCategory || difficulty != mDifficulty || type != mQuestionType) {
            mCategory = categoryId;
            mDifficulty = difficulty;
            mQuestionType = type;

            flush();
        }
        if (mQuestions.isEmpty())
            fetchQuestions(mCategory, 2);
    }

    private class OpenTriviaDatabaseTokenResponse {
        int response_code;
        String response_message;
        String token;
    }

    private void getSessionToken() {
        GsonRequest<OpenTriviaDatabaseTokenResponse> request = new GsonRequest<>(
                SESSION_TOKEN_REQUEST_URL, OpenTriviaDatabaseTokenResponse.class, null,
                new Response.Listener<OpenTriviaDatabaseTokenResponse>() {
                    @Override
                    public void onResponse(OpenTriviaDatabaseTokenResponse response) {
                        switch (response.response_code) {
                            case RESPONSE_CODE_SUCCESS:
                                mSessionToken = response.token;
                                break;
                            default:
                                Log.wtf(TAG, "Failed to request session token: " + response.response_code);
                                mHandler.removeCallbacks(mRetryGetSessionToken);
                                mHandler.postDelayed(mRetryGetSessionToken, RETRY_DELAY_MS);
                                break;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Error while requesting session token: " + error);
                mHandler.removeCallbacks(mRetryGetSessionToken);
                mHandler.postDelayed(mRetryGetSessionToken, RETRY_DELAY_MS);
            }
        });
        requestQueue.add(request);
    }

    private Runnable mRetryGetSessionToken = new Runnable() {
        @Override
        public void run() {
            getSessionToken();
        }
    };

    private class ResetTokenResponse {
        int response_code;
        String token;
    }

    private void resetSessionToken() {
        String url = String.format(SESSION_TOKEN_RESET_URL, mSessionToken);
        Log.v(TAG, "resetSessionToken url=" + url);
        GsonRequest<ResetTokenResponse> request = new GsonRequest<>(url, ResetTokenResponse.class,
                null,
                new Response.Listener<ResetTokenResponse>() {
                    @Override
                    public void onResponse(ResetTokenResponse response) {
                        switch (response.response_code) {
                            case RESPONSE_CODE_SUCCESS:
                                mSessionToken = response.token;
                                break;
                            default:
                                Log.wtf(TAG, "Failed to reset session token: " + response.response_code);
                                scheduleRetryResetSessionToken();
                                break;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Error while resetting session token: " + error);
                scheduleRetryResetSessionToken();
            }
        });
        requestQueue.add(request);
    }

    private void scheduleRetryResetSessionToken() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                resetSessionToken();
            }
        }, RETRY_DELAY_MS);
    }

    private class Category {
        int id;
        String name;
    }

    private class CategoriesResponse {
        Category trivia_categories[];
    }

    private void fetchCategories() {
        GsonRequest<CategoriesResponse> request = new GsonRequest<>(CATEGORY_LOOKUP_URL, CategoriesResponse.class,
                null,
                new Response.Listener<CategoriesResponse>() {
                    @Override
                    public void onResponse(CategoriesResponse response) {
                        mCategories.clear();
                        for (Category category : response.trivia_categories) {
                            mCategories.put(category.id, category.name);
                        }
                        List<String> list = asList(mCategories);
                        if (mListener != null) {
                            mListener.onCategoriesChanged(list);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Error while fetching categories: " + error);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchCategories();
                    }
                }, RETRY_DELAY_MS);
            }
        });
        requestQueue.add(request);
    }

    public SparseArray<String> getCategories() {
        return mCategories;
    }

    /**
     * Creates a new ArrayAdapter from external resources. The content of the array is
     * obtained through {@link android.content.res.Resources#getTextArray(int)}.
     *
     * @param textViewResId The identifier of the layout used to create views.
     *
     * @return An ArrayAdapter<CharSequence>.
     */
    public @NonNull
    ArrayAdapter<CharSequence> createFromResource(@LayoutRes int textViewResId) {
        return ArrayAdapter.createFromResource(mContext, R.array.question_difficulty_array, textViewResId);
    }

    private class QuestionCount {
        int total_question_count = 0;
        int total_easy_question_count = 0;
        int total_medium_question_count = 0;
        int total_hard_question_count = 0;

        void reset() {
            total_question_count = total_easy_question_count = total_medium_question_count = total_hard_question_count = 0;
        }

        int get() {
            switch (mDifficulty) {
                case DIFFICULTY_EASY:
                    return total_easy_question_count;
                case DIFFICULTY_MEDIUM:
                    return total_medium_question_count;
                case DIFFICULTY_HARD:
                    return total_hard_question_count;
                default:
                    return total_question_count;
            }
        }
    }

    private class QuestionCountResponse {
        int category_id;
        QuestionCount category_question_count;
    }

    private void fetchQuestionCount(final int category) {
        String url = new Uri.Builder()
                .scheme(API_SCHEME)
                .authority(API_AUTHORITY)
                .appendPath(CATEGORY_QUESTION_COUNT_LOOKUP_URL)
                .appendQueryParameter(CATEGORY_KEY, Integer.toString(category))
                .build().toString();
        Log.v(TAG, "fetchQuestionCount url=" + url);
        GsonRequest<QuestionCountResponse> request = new GsonRequest<>(url, QuestionCountResponse.class,
                null,
                new Response.Listener<QuestionCountResponse>() {
                    @Override
                    public void onResponse(QuestionCountResponse response) {
                        if (response == null)
                            mQuestionCount.reset();
                        else
                            mQuestionCount = response.category_question_count;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Error while fetching question count: " + error);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchQuestionCount(category);
                    }
                }, RETRY_DELAY_MS);
            }
        });
        requestQueue.add(request);
    }

    private class GlobalQuestionCount {
        private class GlobalQuestionCountDetails {
            int total_num_of_questions;
            int total_num_of_pending_questions;
            int total_num_of_verified_questions;
            int total_num_of_rejected_questions;
        }
        GlobalQuestionCountDetails overall;
        SparseArray<GlobalQuestionCountDetails> categories = new SparseArray<>();
    }
    private GlobalQuestionCount mGlobalQuestionCount = new GlobalQuestionCount();

    private void fetchQuestionCount() {
        String url = new Uri.Builder()
                .scheme(API_SCHEME)
                .authority(API_AUTHORITY)
                .appendPath(GLOBAL_QUESTION_COUNT_LOOKUP_URL)
                .build().toString();
        Log.v(TAG, "fetchQuestionCount url=" + url);
        JsonObjectRequest request = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v(TAG, "Response: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });
        /*
        GsonRequest<QuestionCountResponse> request = new GsonRequest<>(url, QuestionCountResponse.class,
                null,
                new Response.Listener<QuestionCountResponse>() {
                    @Override
                    public void onResponse(QuestionCountResponse response) {
                        if (response == null)
                            mQuestionCount.reset();
                        else
                            mQuestionCount = response.category_question_count;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Error while fetching question count: " + error);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchQuestionCount(category);
                    }
                }, RETRY_DELAY_MS);
            }
        });
        */
        requestQueue.add(request);
    }

    class Question {
        int category;
        int type;
        int difficulty;
        String question;
        String correct_answer;
        String incorrect_answers[];
    }

    class QuestionDetails {
        String category;
        String type;
        String difficulty;
        String question;
        String correct_answer;
        String incorrect_answers[];
    }

    private class QuestionsResponse {
        int response_code;
        QuestionDetails results[];
    }

    private void fetchQuestions(final int category, final int num) {
        Uri.Builder builder = new Uri.Builder()
                .scheme(API_SCHEME)
                .authority(API_AUTHORITY)
                .appendPath(API_PATH)
                .appendQueryParameter(AMOUNT_KEY, Integer.toString(num));
        if (category >= 0) {
            builder.appendQueryParameter(CATEGORY_KEY, Integer.toString(category));
        }
        if (mQuestionType != TYPE_ANY) {
            builder.appendQueryParameter(TYPE_KEY, TYPE_VALUES[mQuestionType]);
        }
        if (mDifficulty != DIFFICULTY_ANY) {
            builder.appendQueryParameter(DIFFICULTY_KEY, DIFFICULTY_VALUES[mDifficulty]);
        }
        if (mEncodingType != ENCODING_DEFAULT) {
            builder.appendQueryParameter(ENCODING_KEY, ENCODING_VALUES[mEncodingType]);
        }
        if (mSessionToken != null) {
            builder.appendQueryParameter(TOKEN_KEY, mSessionToken);
        }
        String url = builder.build().toString();
        Log.v(TAG, "fetchQuestions url=" + url);
        GsonRequest<QuestionsResponse> request = new GsonRequest<>(url, QuestionsResponse.class, null,
                new Response.Listener<QuestionsResponse>() {
                    @Override
                    public void onResponse(QuestionsResponse response) {
                        switch (response.response_code) {
                            case RESPONSE_CODE_SUCCESS:
                                boolean wasEmpty = mQuestions.isEmpty();
                                for (QuestionDetails details: response.results) {
                                    Question question = new Question();
                                    int index = mCategories.indexOfValue(details.category);
                                    if (index < 0) {
                                        question.category = CATEGORY_ANY;
                                    } else {
                                        question.category = mCategories.keyAt(index);
                                    }
                                    if (details.type.equals(TYPE_VALUES[TYPE_MULTIPLE_CHOICE]))
                                        question.type = TYPE_MULTIPLE_CHOICE;
                                    else if (details.type.equals(TYPE_VALUES[TYPE_TRUE_OR_FALSE]))
                                        question.type = TYPE_TRUE_OR_FALSE;
                                    else
                                        question.type = TYPE_ANY;
                                    if (details.difficulty.equals(DIFFICULTY_VALUES[DIFFICULTY_EASY]))
                                        question.difficulty = DIFFICULTY_EASY;
                                    else if (details.difficulty.equals(DIFFICULTY_VALUES[DIFFICULTY_MEDIUM]))
                                        question.difficulty = DIFFICULTY_MEDIUM;
                                    else if (details.difficulty.equals(DIFFICULTY_VALUES[DIFFICULTY_HARD]))
                                        question.difficulty = DIFFICULTY_HARD;
                                    else
                                        question.difficulty = DIFFICULTY_ANY;
                                    question.question = decode(details.question);
                                    question.correct_answer = decode(details.correct_answer);
                                    question.incorrect_answers = details.incorrect_answers;
                                    for (int i = 0; i < question.incorrect_answers.length; i++) {
                                        question.incorrect_answers[i] = decode(question.incorrect_answers[i]);
                                    }
                                    mQuestions.add(question);
                                }
                                if (wasEmpty && !mQuestions.isEmpty() && mListener != null) {
                                    mListener.onQuestionsAvailable(true);
                                }
                                break;
                            case RESPONSE_CODE_TOKEN_NOT_FOUND:
                                mHandler.removeCallbacks(mRetryGetSessionToken);
                                getSessionToken();
                                scheduleRetryFetchQuestions(category, num);
                                break;
                            default:
                                Log.wtf(TAG, "Failed to fetch questions: " + response.response_code);
                                break;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Error while fetching categories: " + error);
                scheduleRetryFetchQuestions(category, num);
            }
        });
        requestQueue.add(request);
    }

    private void scheduleRetryFetchQuestions(final int category, final int num) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchQuestions(category, num);
            }
        }, RETRY_DELAY_MS);
    }

    Question getQuestion() {
        if (mQuestions.isEmpty())
            return null;
        Question question = mQuestions.remove(0);
        if (mQuestions.isEmpty()) {
            if (mListener != null)
                mListener.onQuestionsAvailable(false);
            fetchQuestions(mCategory, 2);
        }
        return question;
    }

    public interface Listener {
        void onCategoriesChanged(List<String> categories);
        void onQuestionsAvailable(boolean available);
    }

    private static <C> List<C> asList(SparseArray<C> sparseArray) {
        if (sparseArray == null) return null;
        List<C> arrayList = new ArrayList<>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++)
            arrayList.add(sparseArray.valueAt(i));
        return arrayList;
    }

    void setListener(Listener listener) {
        mListener = listener;
        if (mCategories.size() != 0) {
            listener.onCategoriesChanged(asList(mCategories));
        }
    }
}
