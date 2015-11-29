package com.clevertap.demo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.util.Log;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;
import java.util.Arrays;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PersonalityTypeFormFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PersonalityTypeFormFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PersonalityTypeFormFragment extends Fragment {

    private final List<String> pTypes = Arrays.asList("earth", "fire", "metal", "water", "wood");

    int earthValue = 0;
    int fireValue = 0;
    int metalValue = 0;
    int waterValue = 0;
    int woodValue = 0;

    Boolean submitting = false;

    private OnFragmentInteractionListener mListener;

    MainActivity parentActivity;

    public PersonalityTypeFormFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PersonalityTypeFormFragment.
     */

    public static PersonalityTypeFormFragment newInstance() {
        PersonalityTypeFormFragment fragment = new PersonalityTypeFormFragment();
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
        // Inflate the layout for this fragment
        LayoutInflater lf = getActivity().getLayoutInflater();
        View view = lf.inflate(R.layout.fragment_personality_type_form, container, false);

        SeekBar earthSeekBar = (SeekBar) view.findViewById(R.id.earth_seekbar);
        earthSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                earthValue = progressValue;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        SeekBar fireSeekBar = (SeekBar) view.findViewById(R.id.fire_seekbar);
        fireSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                fireValue = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        SeekBar metalSeekBar = (SeekBar) view.findViewById(R.id.metal_seekbar);
        metalSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                metalValue = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        SeekBar waterSeekBar = (SeekBar) view.findViewById(R.id.water_seekbar);
        waterSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                waterValue = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        SeekBar woodSeekBar = (SeekBar) view.findViewById(R.id.wood_seekbar);
        woodSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                woodValue = progressValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        Button submitButton = (Button) view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSubmitPressed();
            }
        });

        getActivity().setTitle(R.string.title_pt_form);

        if(!parentActivity.getHasSeenInstructions()) {
            showInstructionsDialog();
            parentActivity.setHasSeenInstructions(true);
        }

        return view;
    }

    public void onSubmitPressed() {

        if(submitting) {
            return ;
        }

        submitting = true;

        List<Integer> scores = Arrays.asList(earthValue, fireValue, metalValue, waterValue, woodValue);

        int winningIdx = 0;
        int highScore = 0;

        for (int i = 0; i < 5; i++) {
            int score = scores.get(i);
            if(score > highScore) {
                winningIdx = i;
                highScore = score;
            }

        }
        String newType = pTypes.get(winningIdx);

        if (mListener != null && newType != null) {
            mListener.onFragmentInteractionPersonalityTypeForm(newType);
        } else {
            submitting = false;
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

    private void showInstructionsDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getContext());

        // set title
        alertDialogBuilder.setTitle(R.string.welcome);

        // set dialog message
        alertDialogBuilder
                .setMessage(R.string.instructions)
                .setCancelable(false)
                .setPositiveButton(R.string.close_instructions,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }
}
