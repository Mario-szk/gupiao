package com.example.mapper;

import java.util.List;

import com.example.model.RobotAccountDo;

/**
 * com.example.mappe.GuPiaoMapper
 * @author king
 *
 */
public interface RobotAccountMapper {
	
	List<RobotAccountDo> getAll();

	int insert(RobotAccountDo obj);

	int delete(Long id);
}
