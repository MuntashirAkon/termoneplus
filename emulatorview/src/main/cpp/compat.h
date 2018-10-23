#ifndef TERMONEPLUS_COMPAT_H
#define TERMONEPLUS_COMPAT_H
/*
 * Copyright (C) 2018 Roumen Petrov.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * NAME
 *   closefrom -- delete open file descriptors
 *
 * SYNOPSIS
 *   void closefrom(int lowfd);
 *
 * DESCRIPTION
 *   The closefrom() system call deletes all open file descriptors greater
 *   than or equal to lowfd from the per-process object	reference table.  Any
 *   errors encountered	while closing file descriptors are ignored.
 */
void closefrom(int lowfd);

#endif /* ndef TERMONEPLUS_COMPAT_H */
