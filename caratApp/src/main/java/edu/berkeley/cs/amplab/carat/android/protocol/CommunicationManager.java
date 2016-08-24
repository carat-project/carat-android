package edu.berkeley.cs.amplab.carat.android.protocol;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;

import com.flurry.android.FlurryAgent;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.thrift.Answers;
import edu.berkeley.cs.amplab.carat.thrift.CaratService;
import edu.berkeley.cs.amplab.carat.thrift.Feature;
import edu.berkeley.cs.amplab.carat.thrift.HogBugReport;
import edu.berkeley.cs.amplab.carat.thrift.ProcessInfo;
import edu.berkeley.cs.amplab.carat.thrift.Questionnaire;
import edu.berkeley.cs.amplab.carat.thrift.Registration;
import edu.berkeley.cs.amplab.carat.thrift.Reports;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

import edu.berkeley.cs.amplab.carat.android.protocol.ProtocolClient.ServerLocation;

public class CommunicationManager {

	private static final String TAG = "CommsManager";
	private static final String DAEMONS_URL = "http://carat.cs.helsinki.fi/daemons.txt";
	private static final String QUESTIONNAIRE_URL = "http://www.cs.helsinki.fi/u/lagerspe/caratapp/questionnaire-url.txt";

	private CaratApplication a = null;

	private boolean registered = false;
	private boolean register = true;
	private boolean newuuid = false;
	private boolean timeBasedUuid = false;
	private boolean gettingReports = false;
	private SharedPreferences p = null;

	public CommunicationManager(CaratApplication a) {
		this.a = a;
		p = PreferenceManager.getDefaultSharedPreferences(this.a);

		/*
		 * Either: 1. Never registered -> register 2. registered, but no stored
		 * uuid -> register 3. registered, with stored uuid, but uuid, model or
		 * os are different -> register 4. registered, all fields equal to
		 * stored -> do not register
		 */
		timeBasedUuid = p.getBoolean(Constants.PREFERENCE_TIME_BASED_UUID, false);
		newuuid = p.getBoolean(Constants.PREFERENCE_NEW_UUID, false);
		registered = !p.getBoolean(Constants.PREFERENCE_FIRST_RUN, true);
		register = !registered;
		String storedUuid = p.getString(CaratApplication.getRegisteredUuid(), null);
		if (!register) {
			if (storedUuid == null)
				register = true;
			else {
				String storedOs = p.getString(Constants.REGISTERED_OS, null);
				String storedModel = p.getString(Constants.REGISTERED_MODEL, null);

				String uuid = storedUuid;

				String os = SamplingLibrary.getOsVersion();
				String model = SamplingLibrary.getModel();

				// need to re-reg
				register = storedUuid == null || os == null || model == null || storedModel == null || storedOs == null
						|| uuid == null || !(storedOs.equals(os) && storedModel.equals(model));
			}
		}
	}

	private void registerMe(CaratService.Client instance, String uuId, String os, String model, String countryCode) throws TException {
		if (uuId == null || os == null || model == null) {
			Log.e("registerMe", "Null uuId, os, or model given to registerMe!");
			System.exit(1);
			return;
		}
		Registration registration = new Registration(uuId);
		registration.setPlatformId(model);
		registration.setSystemVersion(os);
		registration.setTimestamp(System.currentTimeMillis() / 1000.0);
		registration.setKernelVersion(SamplingLibrary.getKernelVersion());
		registration.setSystemDistribution(SamplingLibrary.getManufacturer() + ";" + SamplingLibrary.getBrand());
		FlurryAgent.logEvent("Registering " + uuId + "," + model + "," + os);
		instance.registerMe(registration);
	}

	public int uploadSamples(Collection<Sample> samples) {
		CaratService.Client instance = null;
		int succeeded = 0;
		ArrayList<Sample> samplesLeft = new ArrayList<Sample>();
		registerLocal();
		try {
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.GLOBAL);
			registerOnFirstRun(instance);

			for (Sample s : samples) {
				boolean success = false;
				try {
					success = instance.uploadSample(s);
				} catch (Throwable th) {
					Log.e(TAG, "Error uploading sample.", th);
				}
				if (success)
					succeeded++;
				else
					samplesLeft.add(s);
			}

			safeClose(instance);
		} catch (Throwable th) {
			Log.e(TAG, "Error refreshing main reports.", th);
			safeClose(instance);
		}
		// Do not try again. It can cause a massive sample attack on the server.
		return succeeded;
	}

	private void registerLocal() {
		if (register) {
			String uuId = p.getString(CaratApplication.getRegisteredUuid(), null);
			if (uuId == null) {
				if (registered && (!newuuid && !timeBasedUuid)) {
					uuId = SamplingLibrary.getAndroidId(a);
				} else if (registered && !timeBasedUuid) {
					// "new" uuid
					uuId = SamplingLibrary.getUuid(a);
				} else {
					// Time-based ID scheme
					uuId = SamplingLibrary.getTimeBasedUuid(a);
					if (Constants.DEBUG)
					    Log.d("CommunicationManager", "Generated a new time-based UUID: " + uuId);
					// This needs to be saved now, so that if server
					// communication
					// fails we have a stable UUID.
					p.edit().putString(CaratApplication.getRegisteredUuid(), uuId).commit();
					p.edit().putBoolean(Constants.PREFERENCE_TIME_BASED_UUID, true).commit();
					timeBasedUuid = true;
				}
			}
		}
	}

	private void registerOnFirstRun(CaratService.Client instance) {
		if (register) {
			String uuId = p.getString(CaratApplication.getRegisteredUuid(), null);
			// Only use new uuid if reg'd after this version for the first time.
			if (registered && (!newuuid && !timeBasedUuid)) {
				uuId = SamplingLibrary.getAndroidId(a);
			} else if (registered && !timeBasedUuid) {
				// "new" uuid
				uuId = SamplingLibrary.getUuid(a);
			} else {
				// Time-based ID scheme
				if (uuId == null)
					uuId = SamplingLibrary.getTimeBasedUuid(a);
				if (Constants.DEBUG)
				    Log.d("CommunicationManager", "Generated a new time-based UUID: " + uuId);
				// This needs to be saved now, so that if server communication
				// fails we have a stable UUID.
				p.edit().putString(CaratApplication.getRegisteredUuid(), uuId).commit();
				p.edit().putBoolean(Constants.PREFERENCE_TIME_BASED_UUID, true).commit();
				timeBasedUuid = true;
			}
			String os = SamplingLibrary.getOsVersion();
			String model = SamplingLibrary.getModel();
			String countryCode = SamplingLibrary.getCountryCode(a.getApplicationContext());
			if (Constants.DEBUG)
			    Log.d("CommunicationManager", "First run, registering this device: " + uuId + ", " + os + ", " + model);
			try {
				registerMe(instance, uuId, os, model, countryCode);
				p.edit().putBoolean(Constants.PREFERENCE_FIRST_RUN, false).commit();
				register = false;
				registered = true;
				p.edit().putString(CaratApplication.getRegisteredUuid(), uuId).commit();
				p.edit().putString(Constants.REGISTERED_OS, os).commit();
				p.edit().putString(Constants.REGISTERED_MODEL, model).commit();
			} catch (TException e) {
				Log.e("CommunicationManager", "Registration failed, will try again next time: " + e);
				e.printStackTrace();
			}
		}
	}

	// Flag to check if there is an ongoing refresh
	public boolean isRefreshingReports(){
		return gettingReports;
	}

	/**
	 * Used by UiRefreshThread which needs to know about exceptions.
	 * 
	 * @throws TException
	 */
	public synchronized boolean refreshAllReports() {
		registerLocal();
		// Do not refresh if not connected
		if (!SamplingLibrary.networkAvailable(a.getApplicationContext())){
			return false;
		}
		if (System.currentTimeMillis() - CaratApplication.getStorage().getFreshness() < Constants.FRESHNESS_TIMEOUT){
			return false;
		} else {
			if(Constants.DEBUG){
				Log.d(TAG, "Enough time passed, time to check for new reports.");
			}
		}
		// Establish connection
		if (register) {
			CaratService.Client instance = null;
			try {
				instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.GLOBAL);
				registerOnFirstRun(instance);
				safeClose(instance);
			} catch (Throwable th) {
				Log.e(TAG, "Error refreshing main reports.", th);
				safeClose(instance);
			}
		}

		String uuId = p.getString(CaratApplication.getRegisteredUuid(), null);
		String model = SamplingLibrary.getModel();
		String OS = SamplingLibrary.getOsVersion();
		String countryCode = SamplingLibrary.getCountryCode(a.getApplicationContext());

		// NOTE: Fake data for simulator
		if (model.equals("sdk")) {
			uuId = "97c542cd8e99d948"; // My S3
			model = "GT-I9300";
			OS = "4.0.4";
		}
		if (Constants.DEBUG){
			Log.d(TAG, "Getting reports for " + uuId + " model=" + model + " os=" + OS);
		}
		FlurryAgent.logEvent("Getting reports for " + uuId + "," + model + "," + OS);

		int progress = 0;
		String[] titles = CaratApplication.getTitles();
		if (titles != null){
			String[] temp = Arrays.copyOfRange(titles, 2, 5);
			titles = temp;
		}

		// This flag is used to make sure action progress
		// is not changed while updating is happening
		gettingReports = true;

		//
		// Main reports
		//
		CaratApplication.setStatus(a.getString(R.string.updating) + " " + titles[0]);
		boolean mainSuccess = refreshMainReports(uuId, OS, model);
		if (mainSuccess) {
			progress += 20;
			CaratApplication.setStatus(a.getString(R.string.updating) + " " + titles[1]);
			if (Constants.DEBUG)
			    Log.d(TAG, "Successfully got main report");
		} else {
			CaratApplication.setStatus(a.getString(R.string.updating) + " " + titles[0]);
			if (Constants.DEBUG)
			    Log.d(TAG, "Failed getting main report");
		}

		//
		// Bug reports
		//
		boolean bugsSuccess = refreshBugReports(uuId, model);
		
		if (bugsSuccess) {
			progress += 20;
			CaratApplication.setStatus(a.getString(R.string.updating) + " " + titles[2]);
			if (Constants.DEBUG)
			    Log.d(TAG, "Successfully got bug report");
		} else {
			CaratApplication.setStatus(a.getString(R.string.updating) + " " + titles[1]);
			if (Constants.DEBUG)
			    Log.d(TAG, "Failed getting bug report");
		}

		//
		// Hog reports
		//
		boolean hogsSuccess = refreshHogReports(uuId, model);

		boolean blacklistShouldBeRefreshed = true;
		if (System.currentTimeMillis() - CaratApplication.getStorage().getBlacklistFreshness() < Constants.FRESHNESS_TIMEOUT_BLACKLIST)
			blacklistShouldBeRefreshed = false;

		if (hogsSuccess) {
			progress += 40; // changed to 40
			CaratApplication.setStatus(
					blacklistShouldBeRefreshed ?
							a.getString(R.string.updating) + " " + a.getString(R.string.blacklist)
							: a.getString(R.string.finishing));
			if (Constants.DEBUG)
			    Log.d(TAG, "Successfully got hog report");
		} else {
			CaratApplication.setStatus(a.getString(R.string.updating) + " " + titles[2]);
			if (Constants.DEBUG)
			    Log.d(TAG, "Failed getting hog report");
		}
		
		// NOTE: Check for having a J-Score, and in case there is none, send the
		// new message. Also check if normal hogs exist.
		Reports r = CaratApplication.getStorage().getReports();
		boolean hogsEmpty = CaratApplication.getStorage().hogsIsEmpty();
		boolean quickHogsSuccess = false;
		if (r == null || r.jScoreWith == null || r.jScoreWith.expectedValue <= 0 || hogsEmpty) {
            quickHogsSuccess = getQuickHogsAndMaybeRegister(uuId, OS, model, countryCode);
            if (Constants.DEBUG) {
                if (quickHogsSuccess)
                    Log.d(TAG, "Got quickHogs.");
                else
                    Log.d(TAG, "Failed getting GuickHogs.");
            }
		}

		// Upload all answers for different questionnaires, there should
		// not be many (most of the time none) so doing this here is ok.
		uploadAnswers();
		boolean questionnairesDisabled = p.getBoolean("noQuestionnaires", false);
		if(!questionnairesDisabled){
			getQuestionnaires(uuId);
		}

		if (blacklistShouldBeRefreshed) {
			refreshBlacklist();
			refreshQuestionnaireLink();
		}

		gettingReports = false;

		CaratApplication.refreshStaticActionCount();

		// Only write freshness if we managed to get something
		if(mainSuccess || hogsSuccess || bugsSuccess || quickHogsSuccess){
			CaratApplication.getStorage().writeFreshness();
			if (Constants.DEBUG) Log.d(TAG, "Wrote freshness");
			return true;
		}
		return false;
	}

	private boolean refreshMainReports(String uuid, String os, String model) {
		if (System.currentTimeMillis() - CaratApplication.getStorage().getFreshness() < Constants.FRESHNESS_TIMEOUT)
			return false;
		CaratService.Client instance = null;
		try {
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.GLOBAL);
			Reports r = instance.getReports(uuid, getFeatures("Model", model, "OS", os));
			// Assume multiple invocations, do not close
			// ProtocolClient.close();
			if (r != null) {
			    if (Constants.DEBUG) Log.d("CommunicationManager.refreshMainReports()",
						"got the main report (action list)" + ", model=" + r.getModel()
						+ ", jscore=" + r.getJScore() + ". Storing the report in the databse");
				CaratApplication.getStorage().writeReports(r);
			} else {
			    if (Constants.DEBUG)
			        Log.d("CommunicationManager.refreshMainReports()",
						"the fetched MAIN report is null");
			}
			// Assume freshness written by caller.
			// s.writeFreshness();
			safeClose(instance);
			return true;
		} catch (Throwable th) {
			Log.e(TAG, "Error refreshing main reports.", th);
			safeClose(instance);
		}
		return false;
	}

	private boolean refreshBugReports(String uuid, String model) {
		if (System.currentTimeMillis() - CaratApplication.getStorage().getFreshness() < Constants.FRESHNESS_TIMEOUT)
			return false;
		CaratService.Client instance = null;
		try {
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.GLOBAL);
			HogBugReport r = instance.getHogOrBugReport(uuid, getFeatures("ReportType", "Bug", "Model", model));
			// Assume multiple invocations, do not close
			// ProtocolClient.close();

			// Do not write empty bugs either
			if (r != null && !r.getHbList().isEmpty()) {
				CaratApplication.getStorage().writeBugReport(r);
				if (Constants.DEBUG)
				    Log.d("CommunicationManager.refreshBugReports()", 
						"got the bug list: " + r.getHbList().toString());
			} else {
			    if (Constants.DEBUG)
			        Log.d("CommunicationManager.refreshBugReports()", 
						"the fetched bug report is null");
			}
			safeClose(instance);
			return true;
		} catch (Throwable th) {
			Log.e(TAG, "Error refreshing bug reports.", th);
			safeClose(instance);
		}
		return false;
	}

	private boolean refreshHogReports(String uuid, String model) {
		if (System.currentTimeMillis() - CaratApplication.getStorage().getFreshness() < Constants.FRESHNESS_TIMEOUT)
			return false;
		CaratService.Client instance = null;
		try {
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.GLOBAL);
			HogBugReport r = instance.getHogOrBugReport(uuid, getFeatures("ReportType", "Hog", "Model", model));

			// Assume multiple invocations, do not close
			// ProtocolClient.close();

			// Do not write empty hogs, it clears the quick hogs!
			if (r != null && !r.getHbList().isEmpty()) {
				CaratApplication.getStorage().writeHogReport(r);
				if (Constants.DEBUG)
				    Log.d("CommunicationManager.refreshHogReports()", 
						"got the hog list: " + r.getHbList().toString());
			} else {
			    if (Constants.DEBUG)
			        Log.d("CommunicationManager.refreshHogReports()", 
						"the fetched hog report is null");
			}
			// Assume freshness written by caller.
			// s.writeFreshness();
			safeClose(instance);
			return true;
		} catch (Throwable th) {
			Log.e(TAG, "Error refreshing hog reports.", th);
			safeClose(instance);
		}
		return false;
	}

//	private boolean refreshSettingsReports(String uuid, String model) {
//		if (System.currentTimeMillis() - CaratApplication.storage.getFreshness() < Constants.FRESHNESS_TIMEOUT)
//			return false;
//		CaratService.Client instance = null;
//		try {
//			instance = ProtocolClient.open(a.getApplicationContext());
//			HogBugReport r = instance.getHogOrBugReport(uuid, getFeatures("ReportType", "Settings", "Model", model));
//
//			if (r != null) {
//				CaratApplication.storage.writeSettingsReport(r);
//				Log.d("CommunicationManager.refreshSettingsReports()", 
//						"got the settings list: " + r.getHbList().toString());
//			} else {
//				Log.d("CommunicationManager.refreshSettingsReports()", 
//						"the fetched settings report is null");
//			}
//			// Assume freshness written by caller.
//			// s.writeFreshness();
//			safeClose(instance);
//			return true;
//		} catch (Throwable th) {
//			Log.e(TAG, "Error refreshing settings reports.", th);
//			safeClose(instance);
//		}
//		return false;
//	}
	
	private void refreshBlacklist() {
		// I/O, let's do it on the background.
		new Thread() {
			public void run() {
				final List<String> blacklist = new ArrayList<String>();
				final List<String> globlist = new ArrayList<String>();
				try {
					URL u = new URL(DAEMONS_URL);
					URLConnection c = u.openConnection();
					InputStream is = c.getInputStream();
					if (is != null) {
						BufferedReader rd = new BufferedReader(new InputStreamReader(is));
						String s = rd.readLine();
						while (s != null) {
							// Optimization for android: Only add names that
							// have a dot
							// Does not work, since for example "system" has no
							// dots.
							blacklist.add(s);
							if (s.endsWith("*") || s.startsWith("*"))
								globlist.add(s);
							s = rd.readLine();
						}
						rd.close();
						Log.v(TAG, "Downloaded blacklist: " + blacklist);
						Log.v(TAG, "Downloaded globlist: " + globlist);
						CaratApplication.getStorage().writeBlacklist(blacklist);
						// List of *something or something* expressions:
						if (globlist.size() > 0)
							CaratApplication.getStorage().writeGloblist(globlist);

					}
				} catch (Throwable th) {
					Log.e(TAG, "Could not retrieve blacklist!", th);
				}
				// So we don't try again too often.
				CaratApplication.getStorage().writeBlacklistFreshness();
			}
		}.start();
	}

	private void refreshQuestionnaireLink() {
		// I/O, let's do it on the background.
		new Thread() {
			public void run() {
				String s = null;
				try {
					URL u = new URL(QUESTIONNAIRE_URL);
					URLConnection c = u.openConnection();
					InputStream is = c.getInputStream();
					if (is != null) {
						BufferedReader rd = new BufferedReader(new InputStreamReader(is));
						s = rd.readLine();
						rd.close();
						if (s != null && s.length() > 7 && s.startsWith("http"))
							CaratApplication.getStorage().writeQuestionnaireUrl(s);
						else
							CaratApplication.getStorage().writeQuestionnaireUrl(" ");
					}
				} catch (Throwable th) {
					Log.e(TAG, "Could not retrieve blacklist!", th);
				}
			}
		}.start();
	}

	private boolean getQuestionnaires(String uuid){
		double freshness = CaratApplication.getStorage().getQuestionnaireFreshness();
		if(System.currentTimeMillis() - freshness < Constants.FRESHNESS_TIMEOUT_QUESTIONNAIRE) {
			if(Constants.DEBUG){
				long waitFor = (long)(Constants.FRESHNESS_TIMEOUT_QUESTIONNAIRE - (System.currentTimeMillis() - freshness));
				Log.d(TAG, "Still need to wait "+ TimeUnit.MILLISECONDS.toSeconds(waitFor) +"s for next questionnaire check.");
			}
			return false;
		}
		if(Constants.DEBUG){
			Log.d(TAG, "Enough time passed, checking for questionnaires.");
		}
		CaratService.Client instance = null;
		try {
			// This needs to be EU, other servers will not provide meaningful data
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.EU);
			List<Questionnaire> questionnaires = instance.getQuestionnaires(uuid);
			if(questionnaires == null) return false;
			if(Constants.DEBUG){
				Log.d(TAG, "Downloaded questionnaires " + questionnaires);
			}
			questionnaires = filterQuestionnaires(questionnaires);
			checkAndNotify(questionnaires); // Post notification if new
			CaratApplication.getStorage().writeQuestionnaires(questionnaires);
			long timestamp = System.currentTimeMillis();
			CaratApplication.getStorage().writeQuestionnaireFreshness(timestamp);
			safeClose(instance);
			return true;
		} catch (Throwable th){
			if(Constants.DEBUG){
				th.printStackTrace();
			}
			safeClose(instance);
		}
		return false;
	}

	/**
	 * Go through downloaded questionnaires and check if any of them
	 * is not already stored on device. Post a notification when a new
	 * questionnaire is found.
	 * @param questionnaires Downloaded questionnaires
     */
	private void checkAndNotify(List<Questionnaire> questionnaires){
		HashMap<Integer, Questionnaire> stored = CaratApplication.getStorage().getQuestionnaires();
		for(Questionnaire q : questionnaires){
			if(stored == null || stored.get(q.getId()) == null){
				CaratApplication.postNotification(
						"New questionnaire available!",
						"Please open Carat",
						R.id.actions_layout
				);
				return;
			}
		}
	}

	/**
	 * Filter out questionnaires that already have pending answers or a restriction
	 * to how long user needs to have had Carat installed.
	 * @param questionnaires list of questionnaires
	 * @return filtered questionnaires
     */
	private List<Questionnaire> filterQuestionnaires(List<Questionnaire> questionnaires){
		HashMap<Integer, Answers> answers = CaratApplication.getStorage().getAllAnswers();
		boolean answersAvailable = answers != null && !answers.isEmpty();
		List<Questionnaire> filtered = new ArrayList<>();
		for(Questionnaire q : questionnaires){
			if(q.isSetNewUserLimit()){
				long limit = q.getNewUserLimit();
				long installationDate = CaratApplication.getInstallationDate();
				if(System.currentTimeMillis() - installationDate < limit){
					if(!Constants.DEBUG) continue;
				}
			}
			if(answersAvailable){
				Answers a = answers.get(q.getId());
				if(a != null && a.isComplete()){
					continue;
				}
			}
			filtered.add(q);
		}
		return filtered;
	}

	private void uploadAnswers(){
		// Failed submissions are collected in a map and saved back in storage
		// for the next scheduled upload.
		HashMap<Integer, Answers> answerList = CaratApplication.getStorage().getAllAnswers();
		if(answerList == null || answerList.isEmpty()) return;
		List<Answers> failed = new ArrayList<>();
		int successCount = 0;
		for(Answers answers : answerList.values()){
			boolean success = false;
			// Do not submit partial answers
			if(answers.isComplete()){
				success = uploadAnswers(answers);
			}
			if(success){
				successCount++;
			} else {
				failed.add(answers);
			}
		}

		if(Constants.DEBUG){
			Log.d(TAG, "Uploaded " + successCount + "/" + answerList.size() + " answers");
		}

		// Reset freshness here so we can immediately check if new
		// questionnaires have become available as a result of
		// submitting answers.
		if(successCount > 0){
			CaratApplication.getStorage().writeQuestionnaireFreshness(0);
		}

		// Optimally an empty list gets written here
		CaratApplication.getStorage().writeAllAnswers(failed);
	}

	private boolean uploadAnswers(Answers answers){
		if(Constants.DEBUG){
			Log.d(TAG, "Uploading anwers: " + answers);
		}
		CaratService.Client instance = null;
		try {
			// This needs to be EU, other servers will not provide meaningful data
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.EU);
			boolean success = instance.uploadAnswers(answers);
			safeClose(instance);
			return success;
		} catch (Throwable th){
			safeClose(instance);
		}
		return false;
	}

	private boolean getQuickHogsAndMaybeRegister(String uuid, String os, String model, String countryCode) {
		if (System.currentTimeMillis() - CaratApplication.getStorage().getQuickHogsFreshness() < Constants.FRESHNESS_TIMEOUT_QUICKHOGS)
			return false;
		CaratService.Client instance = null;
		try {
			instance = ProtocolClient.open(a.getApplicationContext(), ServerLocation.GLOBAL);
			Registration registration = new Registration(uuid);
			registration.setPlatformId(model);
			registration.setSystemVersion(os);
			registration.setTimestamp(System.currentTimeMillis() / 1000.0);
			List<ProcessInfo> pi = SamplingLibrary.getRunningAppInfo(a.getApplicationContext());
			List<String> processList = new ArrayList<String>();
			for (ProcessInfo p : pi)
				processList.add(p.pName);

			HogBugReport r = instance.getQuickHogsAndMaybeRegister(registration, processList);
			// Assume multiple invocations, do not close
			// ProtocolClient.close();
			if (r != null && !r.getHbList().isEmpty()) {
				CaratApplication.getStorage().writeHogReport(r);
				CaratApplication.getStorage().writeQuickHogsFreshness();
			}
			// Assume freshness written by caller.
			// s.writeFreshness();
			safeClose(instance);
			return true;
		} catch (Throwable th) {
			Log.e(TAG, "Error refreshing main reports.", th);
			safeClose(instance);
		}
		return false;
	}

	public static void safeClose(CaratService.Client c) {
		if (c == null)
			return;
		TProtocol i = c.getInputProtocol();
		TProtocol o = c.getOutputProtocol();
		if (i != null) {
			TTransport it = i.getTransport();
			if (it != null)
				it.close();
		}
		if (o != null) {
			TTransport it = o.getTransport();
			if (it != null)
				it.close();
		}
	}

	private List<Feature> getFeatures(String key1, String val1, String key2, String val2) {
		List<Feature> features = new ArrayList<Feature>();
		if (key1 == null || val1 == null || key2 == null || val2 == null) {
			Log.e("getFeatures", "Null key or value given to getFeatures!");
			System.exit(1);
			return features;
		}
		Feature feature = new Feature();
		feature.setKey(key1);
		feature.setValue(val1);
		features.add(feature);

		feature = new Feature();
		feature.setKey(key2);
		feature.setValue(val2);
		features.add(feature);
		return features;
	}
}
