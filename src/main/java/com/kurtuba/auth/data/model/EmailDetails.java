package com.kurtuba.auth.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailDetails {
	private String sender;
	private String recipient;
	private String msgBody;
	private String subject;
	private String attachment;
}
