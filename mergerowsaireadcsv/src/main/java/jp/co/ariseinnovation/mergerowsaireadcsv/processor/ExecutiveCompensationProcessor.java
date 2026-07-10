package jp.co.ariseinnovation.mergerowsaireadcsv.processor;

import org.springframework.stereotype.Component;

/**
 * 役員給与（02_140）の処理を担当するクラス
 */
@Component
public class ExecutiveCompensationProcessor extends AbstractMergeProcessor {

	@Override
	public boolean isTargetFormId(String formId) {
		return formId.startsWith("02_140");
	}

	@Override
	protected String getItemNameKeyword() {
		return "給与計";
	}

	@Override
	protected String getStartMessageKey() {
		return "info.executive.compensation.processing.start";
	}

	@Override
	protected String getNoTargetMessageKey() {
		return "info.executive.compensation.no.merge.target";
	}

	@Override
	protected String getCompleteMessageKey() {
		return "info.executive.compensation.merge.complete";
	}

	@Override
	public String getProcessorName() {
		return "ExecutiveCompensation";
	}
}
