/*
 * Copyright (c) 2018, vindell (https://github.com/vindell).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.easy4j.pdf;

import java.util.Map;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 * Implementation of wordprocessing m l template functionality.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
public abstract class WordprocessingMLTemplate {
	
	/**
	 * @param template ：模板内容
	 * @param variables ：变量
	 * @return {@link WordprocessingMLPackage} 对象
	 * @throws Exception ：异常对象
	 */
	public abstract WordprocessingMLPackage process(String template, Map<String, Object> variables) throws Exception;
	
}
