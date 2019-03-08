/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import { Component, forwardRef, Input, OnChanges } from '@angular/core';
import { MatFormFieldControl, MatSnackBar } from '@angular/material';
import { NG_VALUE_ACCESSOR } from '@angular/forms';
import { ApiModel } from 'src/app/api/apimodel';

@Component({
    selector: 'mico-form-number',
    templateUrl: './mico-form-number.component.html',
    styleUrls: ['./mico-form-number.component.css'],
    providers: [{
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => MicoFormNumberComponent),
        multi: true
    }, { provide: MatFormFieldControl, useExisting: Number }],
})

export class MicoFormNumberComponent implements OnChanges {

    constructor(
        private snackBar: MatSnackBar,
    ) { }

    content: number;
    minValue: number = NaN;
    maxValue: number = NaN;
    @Input() config: ApiModel;

    ngOnChanges() {
        if (this.config != null) {
            if (this.config.hasOwnProperty('minimum')) {
                this.minValue = Number(this.config.minimum);
            }
            if (this.config.hasOwnProperty('maximum')) {
                this.maxValue = Number(this.config.maximum);
            }
        }
    }

    onChange: any = () => { };

    onTouched: any = () => { };

    get value(): number {
        return this.content;
    }

    set value(val: number) {
        if (!Number.isNaN(val)) {
            this.content = val;
            this.onChange(val);
            this.onTouched();
        }
    }

    onInputChange(input: number) {
        if (input == null) {
            return;
        }
        if (!isNaN(this.maxValue) && input > this.maxValue) {
            input = this.maxValue;
            this.snackBar.open('Input exeeds the maximum value of: ' + this.maxValue, 'Ok', {
                duration: 8000,
            });
        }
        if (!isNaN(this.minValue) && input < this.minValue) {
            input = this.minValue;
            this.snackBar.open('Input is below the minimum value of: ' + this.minValue, 'Ok', {
                duration: 8000,
            });
        }

        this.content = input;
        this.onChange(input);
        this.onTouched();
    }

    registerOnChange(fn) {
        this.onChange = fn;
    }

    registerOnTouched(fn) {
        this.onTouched = fn;
    }

    writeValue(value) {
        const temp: number = Number(value);
        if (!Number.isNaN(temp)) {
            this.value = temp;
        }
    }
}
