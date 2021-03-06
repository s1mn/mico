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

import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { Subscription, from } from 'rxjs';
import { ApiService } from 'src/app/api/api.service';
import { MatDialogRef, MAT_DIALOG_DATA, MatTableDataSource } from '@angular/material';
import { SelectionModel } from '@angular/cdk/collections';
import { ApiObject } from 'src/app/api/apiobject';
import { groupBy, mergeMap, toArray } from 'rxjs/operators';
import { versionComparator } from 'src/app/api/semantic-version';
import { safeUnsubscribe } from 'src/app/util/utils';


enum FilterTypes {
    None,
    Internal,
    External,
}

enum ChoiceTypes {
    single,
    multi,
}


export interface Service {
    name: string;
    shortName: string;
    description: string;
    id: number;
    version: string;
}

@Component({
    selector: 'mico-service-picker',
    templateUrl: './service-picker.component.html',
    styleUrls: ['./service-picker.component.css']
})

export class ServicePickerComponent implements OnInit, OnDestroy {

    filter = FilterTypes.None;
    choiceModel = ChoiceTypes.multi;
    existingDependencies: Set<string> = new Set();

    private serviceSubscription: Subscription;

    displayedColumns: string[] = ['name', 'shortName', 'version', 'description'];
    dataSource;
    selection;

    // used for highlighting
    selectedRowIndex: number = -1;

    constructor(
        public dialogRef: MatDialogRef<ServicePickerComponent>,
        @Inject(MAT_DIALOG_DATA) public data: any,
        private apiService: ApiService,
    ) {

        if (data.filter === 'internal') {
            this.filter = FilterTypes.Internal;
        } else if (data.filter === 'external') {
            this.filter = FilterTypes.External;
        }

        if (data.choice === 'single') {
            this.choiceModel = ChoiceTypes.single;
            this.selection = new SelectionModel<Service>(false, []);
        } else if (data.choice === 'multi') {
            this.choiceModel = ChoiceTypes.multi;
            this.displayedColumns = ['select', 'name', 'shortName', 'version', 'description'];
            this.selection = new SelectionModel<Service>(true, []);
        }

        if (data.existingDependencies != null) {
            data.existingDependencies.forEach(element => {
                this.existingDependencies.add(element.shortName);
            });
        }

        if (data.serviceId != null && data.serviceId !== '') {
            this.existingDependencies.add(data.serviceId);
        }
    }

    ngOnInit() {


        // get the list of services
        this.serviceSubscription = this.apiService.getServices()
            .subscribe(services => {

                const tempServiceGroups: any[] = [];
                let counter = 0;

                from(services as unknown as ArrayLike<ApiObject>)
                    .pipe(
                        groupBy(service => service.shortName),
                        mergeMap(group => group.pipe(toArray())))
                    .subscribe(group => {

                        // sort descending
                        group.sort((v1, v2) => (-1) * versionComparator(v1.version, v2.version));

                        // filter
                        if (this.filterElement(group[0])) {

                            tempServiceGroups.push(
                                {
                                    id: counter,
                                    name: group[0].name,
                                    shortName: group[0].shortName,
                                    allVersions: group,
                                    selectedVersion: group[0].version,
                                    selectedDescription: group[0].description
                                }
                            );

                            counter++;
                        }
                    });

                this.dataSource = new MatTableDataSource(tempServiceGroups);
            });
    }

    ngOnDestroy() {
        safeUnsubscribe(this.serviceSubscription);
    }

    getSelectedService() {

        const tempSelected = [];

        this.selection.selected.forEach(selectedElement => {

            for (const service of selectedElement.allVersions) {
                if ((service as any).version === selectedElement.selectedVersion) {
                    tempSelected.push(service);
                    // next selected service
                    return;
                }
            }
        });

        return tempSelected;
    }

    private filterElement = (element): boolean => {

        let val = false;

        if (!this.existingDependencies.has(element.shortName)) {
            if (this.filter === FilterTypes.None) {
                val = true;
            } else if (this.filter === FilterTypes.Internal) {
                if (!element.external) {
                    val = true;
                }
            } else if (this.filter === FilterTypes.External) {
                if (element.external) {
                    val = true;
                }
            } else {
                val = false;
            }
        }
        return val;
    }

    applyFilter(filterValue: string) {
        this.dataSource.filter = filterValue.trim().toLowerCase();
    }

    isAllSelected() {
        const numSelected = this.selection.selected.length;
        const numRows = this.dataSource.data.length;
        return numSelected === numRows;
    }

    /** Selects all rows if they are not all selected; otherwise clear selection. */
    masterToggle() {
        this.isAllSelected() ?
            this.selection.clear() :
            this.dataSource.data.forEach(row => this.selection.select(row));
    }

    highlight(row) {
        this.selectedRowIndex = row.id;
    }

    updateVersion(pickedVersion, element) {
        element.selectedVersion = pickedVersion;
        element.allVersions.forEach(serviceVersion => {
            if (serviceVersion.version === pickedVersion) {
                element.selectedDescription = serviceVersion.description;
            }
        });
    }

}
