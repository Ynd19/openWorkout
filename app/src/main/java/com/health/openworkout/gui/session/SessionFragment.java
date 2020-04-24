/*
 * Copyright (C) 2020 by olie.xdev@googlemail.com All Rights Reserved
 */

package com.health.openworkout.gui.session;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.health.openworkout.R;
import com.health.openworkout.core.OpenWorkout;
import com.health.openworkout.core.datatypes.TrainingPlan;
import com.health.openworkout.core.datatypes.WorkoutSession;
import com.health.openworkout.gui.datatypes.GenericAdapter;
import com.health.openworkout.gui.datatypes.GenericFragment;
import com.health.openworkout.gui.datatypes.GenericSettingsFragment;

import java.util.List;

public class SessionFragment extends GenericFragment {
    private RecyclerView sessionsView;

    private TrainingPlan trainingPlan;
    private List<WorkoutSession> workoutSessionList;

    private SessionsAdapter sessionsAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_session, container, false);

        sessionsView = root.findViewById(R.id.sessionsView);

        sessionsView.setHasFixedSize(true);
        sessionsView.setLayoutManager(new GridLayoutManager(getContext(), getNumberOfColumns()));

        loadFromDatabase();

        return root;
    }

    @Override
    protected String getTitle() {
        return trainingPlan.getName();
    }

    @Override
    protected GenericAdapter getAdapter() {
        return sessionsAdapter;
    }

    @Override
    protected RecyclerView getRecyclerView() {
        return sessionsView;
    }

    @Override
    protected List getItemList() {
        return workoutSessionList;
    }

    @Override
    protected void onSelectClick(int position) {
        WorkoutSession workoutSession = workoutSessionList.get(position);

        SessionFragmentDirections.ActionSessionFragmentToWorkoutFragment action = SessionFragmentDirections.actionSessionFragmentToWorkoutFragment();
        action.setTitle(workoutSession.getName());
        action.setSessionWorkoutId(workoutSession.getWorkoutSessionId());
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment).navigate(action);
    }

    @Override
    protected void onEditClick(int position) {
        WorkoutSession workoutSession = workoutSessionList.get(position);

        SessionFragmentDirections.ActionSessionsFragmentToSessionSettingsFragment action = SessionFragmentDirections.actionSessionsFragmentToSessionSettingsFragment();
        action.setWorkoutSessionId(workoutSession.getWorkoutSessionId());
        action.setMode(GenericSettingsFragment.SETTING_MODE.EDIT);
        action.setTitle(getString(R.string.label_edit));
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment).navigate(action);
    }

    @Override
    protected void onDeleteClick(int position) {
        Toast.makeText(getContext(), String.format(getString(R.string.label_delete_toast), workoutSessionList.get(position).getName()), Toast.LENGTH_SHORT).show();
        getItemList().remove(position);
        OpenWorkout.getInstance().deleteWorkoutSession(workoutSessionList.get(position));
    }

    @Override
    protected void onAddClick() {
        SessionFragmentDirections.ActionSessionsFragmentToSessionSettingsFragment action = SessionFragmentDirections.actionSessionsFragmentToSessionSettingsFragment();
        action.setTrainingPlanId(trainingPlan.getTrainingPlanId());
        action.setMode(GenericSettingsFragment.SETTING_MODE.ADD);
        action.setTitle(getString(R.string.label_add));
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment).navigate(action);
    }

    @Override
    protected void loadFromDatabase() {
        long trainingPlanId = SessionFragmentArgs.fromBundle(getArguments()).getTrainingPlanId();
        trainingPlan = OpenWorkout.getInstance().getTrainingPlan(trainingPlanId);

        workoutSessionList = trainingPlan.getWorkoutSessions();

        sessionsAdapter = new SessionsAdapter(getContext(), workoutSessionList);
        sessionsAdapter.setMode(getMode());
        sessionsView.setAdapter(sessionsAdapter);
    }

    @Override
    protected void saveToDatabase() {
        for (int i=0; i<workoutSessionList.size(); i++) {
            workoutSessionList.get(i).setOrderNr(i);
            OpenWorkout.getInstance().updateWorkoutSession(workoutSessionList.get(i));
        }
    }

    private int getNumberOfColumns() {
        View view = View.inflate(getContext(), R.layout.item_session, null);
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = view.getMeasuredWidth();
        int count = getResources().getDisplayMetrics().widthPixels / width;
        int remaining = getResources().getDisplayMetrics().widthPixels - width * count;
        if (remaining > width - 15)
            count++;
        return count;
    }
}
