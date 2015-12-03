package com.clevertap.demo;

import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class QuoteFragment extends Fragment {

    private static final String ARG_QUOTE = "quoteText";
    private static final String ARG_PT = "personalityType";

    private String quoteText;

    MainActivity parentActivity;

    public QuoteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param quote the quote text.
     * @return A new instance of fragment QuoteFragment.
     */
    public static QuoteFragment newInstance(String quote ) {
        QuoteFragment fragment = new QuoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUOTE, quote);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            quoteText = getArguments().getString(ARG_QUOTE);

        }

        parentActivity = ((MainActivity)getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LayoutInflater lf = getActivity().getLayoutInflater();
        View view = lf.inflate(R.layout.fragment_quote, container, false);
        TextView textView = (TextView) view.findViewById(R.id.quote_view);

        // set the text color based on the personality type

        int colorId = parentActivity.getPersonalityTypeColorId();
        if(Math.abs(colorId) > 0) {
            textView.setTextColor(colorId);
        }
        textView.setText(quoteText);

        getActivity().setTitle(R.string.app_name);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    };

}
