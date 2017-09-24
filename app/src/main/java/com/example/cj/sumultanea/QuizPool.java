package com.example.cj.sumultanea;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.content.Context.MODE_PRIVATE;

public class QuizPool {
    private static final String TAG = "QuizPool";
    private static final String poolFileName = "quizpool.xml";
    private Context context;
    private List<Entry> entries;
    private Random random;

    public QuizPool(Context context) {
        this.context = context;
        random = new Random();
        refresh();
    }

    public Entry getQuestion() {
        if (entries == null || entries.isEmpty())
            return null;
        return entries.get(random.nextInt(entries.size()));
    }

    private void refresh() {
        File localPoolFile = context.getFileStreamPath(poolFileName);
        // On first run, or whenever the app is updated, copy the default quizpool file from our
        // resources to the app private data. It can be refreshed later with a new copy from
        // internet (todo)
        long lastUpdateTime = 0;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            lastUpdateTime = packageInfo.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }
        long lastModified = localPoolFile.lastModified();
        if (!localPoolFile.exists() || lastModified < lastUpdateTime) {
            try {
                InputStream is = context.getResources().openRawResource(R.raw.quizpool);
                OutputStream os = context.openFileOutput(poolFileName, MODE_PRIVATE);
                byte[] data = new byte[is.available()];
                is.read(data);
                os.write(data);
                is.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        //TODO: launch a background task top download a new version of the quiz pool file

        FileInputStream stream;
        try {
            stream = context.openFileInput(poolFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        try {
            entries = parse(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // We don't use namespaces
    private static final String ns = null;
    private static final String KEY_QUIZ = "quiz";
    private static final String KEY_ENTRY = "entry";
    private static final String KEY_QUESTION = "question";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ANSWER = "answer";
    private static final String KEY_CORRECT = "correct";
    private static final int TYPE_MULTIPLE_CHOICE = 0;
    private static final int TYPE_IDENTIFICATION = 1;
    private static final int TYPE_TRUE_FALSE = 2;

    private List parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readPool(parser);
        } finally {
            in.close();
        }
    }

    private List readPool(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<Entry> entries = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, KEY_QUIZ);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals(KEY_ENTRY)) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    public static class Entry {
        public final String question;
        public final int type;
        public final List<Answer> answers;

        private Entry(String question, int type, List<Answer> answers) {
            this.question = question;
            this.type = type;
            this.answers = answers;
        }
    }

    public static class Answer {
        public final String text;
        public final Boolean correct;

        private Answer(String text, Boolean correct) {
            this.text = text;
            this.correct = correct;
        }
    }

    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, KEY_ENTRY);
        String question = null;
        int type = TYPE_MULTIPLE_CHOICE;
        List<Answer> answers = null;
        switch (parser.getAttributeValue(null, KEY_TYPE)) {
            case "mc":
                type = TYPE_MULTIPLE_CHOICE;
                break;
            case "id":
                type = TYPE_IDENTIFICATION;
                break;
            case "tf":
                type = TYPE_TRUE_FALSE;
                break;
        }
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            switch (parser.getName()) {
                case KEY_QUESTION:
                    question = readQuestion(parser);
                    break;
                case KEY_ANSWER:
                    if (answers == null) {
                        answers = new ArrayList<>();
                    }
                    answers.add(readAnswer(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Entry(question, type, answers);
    }

    private String readQuestion(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, KEY_QUESTION);
        String text = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, KEY_QUESTION);
        return text;
    }

    private Answer readAnswer(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, KEY_ANSWER);
        Boolean correct = false;
        String result = parser.getAttributeValue(null, KEY_CORRECT);
        if (result != null && (result.equals("true") || result.equals("1"))) {
            correct = true;
        }
        String answer = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, KEY_ANSWER);
        return new Answer(answer, correct);
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
