package com.nkanaev.comics.fragment;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.nkanaev.comics.BuildConfig;
import com.nkanaev.comics.R;
import com.nkanaev.comics.activity.MainActivity;
import net.sf.sevenzipjbinding.SevenZip;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AboutFragment extends Fragment implements View.OnClickListener {
    static private class LibraryDescription {
        public final CharSequence name;
        public final CharSequence description;
        public final CharSequence license;
        public final CharSequence owner;
        public final CharSequence link;

        LibraryDescription(CharSequence name, CharSequence description, CharSequence license, CharSequence owner, CharSequence link) {
            this.name = name;
            this.description = description;
            this.license = license;
            this.owner = owner;
            this.link = link;
        }
    }

    private static CharSequence lib7zDetails() {
        SevenZip.Version version = SevenZip.getSevenZipVersion();
        CharSequence out =
                "7-zip version: " + version.version + ", " + version.date + "\n" +
                        "7-Zip-JBinding version: " + SevenZip.getSevenZipJBindingVersion() + "\n" +
                        "Native library initialized: " + SevenZip.isInitializedSuccessfully();
        return alignRight(out);
    }

    private static CharSequence alignRight(CharSequence text) {
        final SpannableString s = new SpannableString(text);
        s.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    private static List<String> dependencyList = null;

    private static CharSequence getDependencyEntries(String... needles) {
        // lazy init
        if (dependencyList == null) {
            dependencyList = new ArrayList();
            try {
                JSONArray jsonarray = new JSONArray(BuildConfig.DEPENDENCIES);
                for (int i = 0; i < jsonarray.length(); i++) {
                    dependencyList.add(jsonarray.getString(i));
                }
            } catch (Exception e) {
                Log.e("AboutFragment#182", "getVersionString()", e);
            }
        }

        // find entries
        String out = "";
        for (Iterator<String> it = dependencyList.iterator(); it.hasNext();) {
            String depString =  it.next();
            for (String needle : needles) {
                if (depString.toLowerCase().contains(needle.toLowerCase())) {
                    out += (out.isEmpty() ? "" : "\n") + depString;
                    it.remove();
                }
            }
        }
        return out.isEmpty() ? "" : TextUtils.concat("\n\n", alignRight(out));
    }

    private static CharSequence getDependencyLeftovers() {
        String out = "";
        for (String depString : dependencyList) {
            out += (out.isEmpty() ? "" : "\n") + depString;
        }
        return out.isEmpty() ? "" : TextUtils.concat("\n\n", alignRight(out));
    }

    private static ArrayList<LibraryDescription> mDescriptions = new ArrayList();

    static {
        mDescriptions.add(
                new LibraryDescription(
                        "Picasso",
                        TextUtils.concat("A powerful image downloading and caching library for Android",
                                getDependencyEntries("Picasso")),
                        "Apache Version 2.0",
                        "Square",
                        "https://github.com/square/picasso"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Junrar",
                        TextUtils.concat("Plain java unrar util",
                                getDependencyEntries("Junrar")),
                        "Unrar license",
                        "Edmund Wagner",
                        "https://github.com/edmund-wagner/junrar"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Apache Commons Compress",
                        TextUtils.concat("Defines an API for working with tar, zip and bzip2 files",
                                getDependencyEntries("commons-compress")),
                        "Apache Version 2.0",
                        "Apache Software Foundation",
                        "https://commons.apache.org/proper/commons-compress/"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Apache Commons Imaging",
                        TextUtils.concat("... previously known as Apache Commons Sanselan, is a library that reads and writes a variety of image formats, including fast parsing of image info (size, color space, ICC profile, etc.) and metadata.",
                                getDependencyEntries("commons-imaging")),
                        "Apache Version 2.0",
                        "Apache Software Foundation",
                        "https://commons.apache.org/proper/commons-imaging/"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "XZ Utils",
                        TextUtils.concat("XZ Utils is free general-purpose data compression software with a high compression ratio",
                                getDependencyEntries("tukaani")),
                        "Public Domain",
                        "Tukaani Developers",
                        "http://tukaani.org/xz/java.html"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "7-Zip-JBinding-4Android",
                        TextUtils.concat("Android library version of 7-Zip-JBinding java wrapper.\n\n",
                                lib7zDetails(),
                                getDependencyEntries("7-Zip-JBinding-4Android")),
                        "GNU LGPL 2.1 or later + unRAR restriction",
                        "Igor Pavlov, Boris Brodski, Fredrik Claesson",
                        "https://github.com/lings03/7-Zip-JBinding-4Android"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Zstd-jni",
                        TextUtils.concat("JNI bindings to Zstd Library",
                                getDependencyEntries("zstd")),
                        "BSD license",
                        "Luben Karavelov",
                        "https://github.com/luben/zstd-jni"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Brotli",
                        TextUtils.concat("Generic-purpose lossless compression algorithm",
                                getDependencyEntries("brotli")),
                        "MIT license",
                        "Google",
                        "https://github.com/google/brotli"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "JP2 for Android",
                        TextUtils.concat("Open-source JPEG-2000 image encoder/decoder for Android based on OpenJPEG",
                                getDependencyEntries("jp2-android")),
                        "BSD (2-clause) license",
                        "ThalesGroup, Keiji",
                        "https://github.com/keiji/JP2ForAndroid"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Natural Sorting for Java",
                        TextUtils.concat("Perform 'natural order' comparisons of strings in Java.",
                                alignRight("\n\nv1.2.0")),
                        "MIT license",
                        "Jagoba Gascón",
                        "https://github.com/jagobagascon/Natural-Sorting-for-Java"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Material Design Icons",
                        "Official icon sets from Google designed under the material design guidelines.",
                        "Apache Version 2.0",
                        "Google",
                        "https://github.com/google/material-design-icons"
                ));
        mDescriptions.add(
                new LibraryDescription(
                        "Android Jetpack",
                        TextUtils.concat("Androidx suite of tools and libraries",
                                getDependencyEntries("androidx", "material")),
                        "Apache Version 2.0",
                        "Google",
                        "https://developer.android.com/jetpack"
                ));

        CharSequence others = getDependencyLeftovers();
        if (others.length()>0)
            mDescriptions.add(new LibraryDescription(
                    "Others",
                    others,
                    "",
                    "",
                    ""
            ));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView aboutVersionHeaderTextView = view.findViewById(R.id.aboutVersionHeader);
        aboutVersionHeaderTextView.setText(aboutVersionHeaderTextView.getText() + ": ");

        TextView aboutVersionTextView = view.findViewById(R.id.aboutVersion);
        aboutVersionTextView.setText(getVersionString());
        aboutVersionTextView.setSelected(true);

        View appLayout = view.findViewById(R.id.about_application);
        appLayout.setTag(getString(R.string.app_link));
        TextView descView = view.findViewById(R.id.aboutDescription);
        descView.setText(" - " + descView.getText());
        appLayout.setOnClickListener(this);

        LinearLayout libsLayout = (LinearLayout) view.findViewById(R.id.about_libraries);
        for (LibraryDescription ld : mDescriptions) {
            View cardView = inflater.inflate(R.layout.card_deps, libsLayout, false);

            ((TextView) cardView.findViewById(R.id.libraryName)).setText(ld.name);
            ((TextView) cardView.findViewById(R.id.libraryCreator)).setText(ld.owner);
            ((TextView) cardView.findViewById(R.id.libraryDescription)).setText(ld.description);
            ((TextView) cardView.findViewById(R.id.libraryLicense)).setText(ld.license);

            cardView.setTag(ld.link);
            cardView.setOnClickListener(this);
            libsLayout.addView(cardView);
        }

        getActivity().setTitle(R.string.menu_about);
        ((MainActivity) getActivity()).setSubTitle("");

        // Disable title area click listener from library view
        View titleContainer = getActivity().findViewById(R.id.action_bar_title_container);
        if (titleContainer != null) {
            titleContainer.setOnClickListener(null);
            titleContainer.setClickable(false);
        }

        return view;
    }

    private String getVersionString() {
        try {
            PackageInfo pi = getActivity()
                    .getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0);
            return pi.versionName + " (" + Integer.toString(pi.versionCode) + ")";
        } catch (Exception e) {
            Log.e("AboutFragment#195", "getVersionString()", e);
            return "";
        }
    }

    @Override
    public void onClick(View v) {
        String link = (String) v.getTag();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
    }
}
