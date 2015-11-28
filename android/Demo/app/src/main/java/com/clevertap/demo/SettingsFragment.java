package com.clevertap.demo;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.Button;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.EditText;
import android.widget.TextView.OnEditorActionListener;

import java.util.NavigableSet;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    MainActivity parentActivity;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     * @return A new instance of fragment SettingsFragment.
     */

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = ((MainActivity)getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        LayoutInflater lf = getActivity().getLayoutInflater();
        View view = lf.inflate(R.layout.fragment_settings, container, false);

        Switch pushSwitch = (Switch) view.findViewById(R.id.push_switch);

        Boolean pushEnabled = parentActivity.getPushEnabled();
        pushSwitch.setChecked(pushEnabled);
        pushSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
               parentActivity.setPushEnabled(isChecked);

            }
        });

        final Switch emailSwitch = (Switch) view.findViewById(R.id.email_switch);

        Boolean emailEnabled = parentActivity.getEmailEnabled();
        emailSwitch.setChecked(emailEnabled);
        emailSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                parentActivity.setEmailEnabled(isChecked);
            }
        });


        EditText editText = (EditText) view.findViewById(R.id.email_address);

        String currentEmail = parentActivity.getEmailAddress();
        if(currentEmail != null) {
            editText.setText(currentEmail);
        }

        editText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    CharSequence email = v.getText();
                    Boolean valid = isEmailValid(email);
                    if(valid) {
                        emailSwitch.setChecked(true);
                        parentActivity.setEmailAddress(email.toString());
                        return false;
                    }
                }
                return true;
            }
        });

        TextView ptTextView = (TextView) view.findViewById(R.id.personality_type);

        String pType = parentActivity.getPersonalityType();

        // set the text color based on the personality type
        int colorId = parentActivity.getPersonalityTypeColorId();
        if(Math.abs(colorId) > 0) {
            ptTextView.setTextColor(colorId);
        }

        if(pType == null) {
            pType = "Not Set";
        }
        ptTextView.setText(pType);

        Button showPTFormButton = (Button) view.findViewById(R.id.show_pt_form_button);
        showPTFormButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onShowPTFormPressed();
            }
        });

        getActivity().setTitle(R.string.title_settings);

        return view;
    }

    public void onShowPTFormPressed() {
        if (mListener != null) {
            mListener.onFragmentInteractionSettings("showPTForm");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteractionSettings(String action);
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}


