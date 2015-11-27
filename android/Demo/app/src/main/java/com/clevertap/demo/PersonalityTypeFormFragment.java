package com.clevertap.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PersonalityTypeFormFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PersonalityTypeFormFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PersonalityTypeFormFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PT = "personalityType";

    private String personalityType;


    private OnFragmentInteractionListener mListener;

    public PersonalityTypeFormFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param personalityType Parameter 1.
     *
     * @return A new instance of fragment PersonalityTypeFormFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PersonalityTypeFormFragment newInstance(String personalityType) {
        PersonalityTypeFormFragment fragment = new PersonalityTypeFormFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PT, personalityType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            personalityType = getArguments().getString(ARG_PT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LayoutInflater lf = getActivity().getLayoutInflater();
        View view = lf.inflate(R.layout.fragment_personality_type_form, container, false);

        Button submitButton = (Button) view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                personalityType = "water";
                onSubmitPressed();
            }
        });


        getActivity().setTitle(R.string.title_pt_form);

        return view;
    }

    public void onSubmitPressed() {
        if (mListener != null && personalityType != null) {
            mListener.onFragmentInteractionPersonalityTypeForm(personalityType);
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
        void onFragmentInteractionPersonalityTypeForm(String personalityType);
    }
}
